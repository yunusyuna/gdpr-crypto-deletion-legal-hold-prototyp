package edu.depaul.grap.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class GrapDao {
    private final JdbcTemplate jdbc;

    public GrapDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ----------------------------
    // Phase 1/2: Users
    // ----------------------------
    public Map<String, Object> createUser(String fullName, String email, String phone) {
        return jdbc.queryForMap(
                "INSERT INTO users(full_name, email, phone) VALUES (?,?,?) " +
                        "RETURNING user_id, created_at",
                fullName, email, phone
        );
    }

    // DB function returns integer
    public int destroyUserKeys(long userId) {
        Integer v = jdbc.queryForObject("SELECT destroy_user_keys(?)", Integer.class, userId);
        return (v == null) ? 0 : v;
    }

    // Optional “guarded” variant (ONLY if you created destroy_user_keys_guarded in SQL)
    public int destroyUserKeysGuarded(long userId) {
        Integer v = jdbc.queryForObject("SELECT destroy_user_keys_guarded(?)", Integer.class, userId);
        return (v == null) ? 0 : v;
    }

    public long restoreUsersFromShadow() {
        Long v = jdbc.queryForObject("SELECT restore_users_from_shadow()", Long.class);
        return (v == null) ? 0L : v;
    }

    public void truncateUsersCascade() {
        jdbc.execute("TRUNCATE users RESTART IDENTITY CASCADE");
    }

    // Helpful GET APIs for UI (list + by id)
    public List<Map<String, Object>> listUsers(int limit) {
        return jdbc.queryForList(
                "SELECT user_id, full_name, email, phone, created_at " +
                        "FROM users ORDER BY user_id DESC LIMIT ?",
                limit
        );
    }

    public Map<String, Object> getUser(long userId) {
        return jdbc.queryForMap(
                "SELECT user_id, full_name, email, phone, created_at " +
                        "FROM users WHERE user_id = ?",
                userId
        );
    }

    public List<Map<String, Object>> getUserKeys(long userId) {
        return jdbc.queryForList(
                "SELECT key_id, purpose, bucket_date, destroyed_at, key_material " +
                        "FROM key_store WHERE user_id = ? " +
                        "ORDER BY bucket_date DESC, purpose ASC",
                userId
        );
    }

    // ----------------------------
    // Phase 4: Backups
    // ----------------------------
    public Map<String, Object> createBackupRun(String backupType, boolean protectedByHold) {
        // DB expects FULL/INCR (per CHECK constraint)
        String reason = protectedByHold ? "protected_by_hold=true at creation" : null;

        return jdbc.queryForMap(
                "INSERT INTO backup_runs(backup_type, started_at, ended_at, protected_by_hold, protected_reason, protected_at) " +
                        "VALUES (?, now(), now(), ?, ?, CASE WHEN ? THEN now() ELSE NULL END) " +
                        "RETURNING backup_id, backup_type, started_at, ended_at, protected_by_hold, protected_reason, protected_at, created_at",
                backupType, protectedByHold, reason, protectedByHold
        );
    }

    public Map<String, Object> addUserToBackup(long backupId, long userId) {
        return jdbc.queryForMap(
                "INSERT INTO backup_user_index(backup_id, user_id) VALUES (?, ?) " +
                        "RETURNING backup_id, user_id",
                backupId, userId
        );
    }

    public List<Map<String, Object>> listBackups(int limit) {
        return jdbc.queryForList(
                "SELECT backup_id, backup_type, started_at, ended_at, protected_by_hold, protected_reason, protected_at, created_at " +
                        "FROM backup_runs ORDER BY backup_id DESC LIMIT ?",
                limit
        );
    }

    public List<Map<String, Object>> listBackupsForUser(long userId) {
        return jdbc.queryForList("""
            SELECT
                b.backup_id,
                b.backup_type,
                b.created_at,
                b.protected_by_hold,
                b.protected_reason,
                b.protected_at,
                bi.first_seen,
                bi.last_seen
            FROM backup_user_index bi
            JOIN backup_runs b ON b.backup_id = bi.backup_id
            WHERE bi.user_id = ?
            ORDER BY b.backup_id DESC
        """, userId);
    }

    public List<Map<String, Object>> listBackups() {
        return jdbc.queryForList("""
            SELECT backup_id, backup_type, started_at, ended_at, created_at,
                   protected_by_hold, protected_reason, protected_at
            FROM backup_runs
            ORDER BY backup_id DESC
        """);
    }

    // ----------------------------
    // Holds (matches your table schema)
    // legal_holds(hold_id, user_id, hold_reason, created_at, released_at)
    // ----------------------------

    public Map<String, Object> placeLegalHold(long userId, String holdReason) {
        return jdbc.queryForMap(
                "INSERT INTO legal_holds(user_id, hold_reason) VALUES (?, ?) " +
                        "RETURNING hold_id, user_id, hold_reason, created_at, released_at",
                userId, holdReason
        );
    }

    public Map<String, Object> releaseLegalHoldByHoldId(long holdId) {
        return jdbc.queryForMap(
                "UPDATE legal_holds " +
                        "SET released_at = now() " +
                        "WHERE hold_id = ? AND released_at IS NULL " +
                        "RETURNING hold_id, user_id, hold_reason, created_at, released_at",
                holdId
        );
    }

    // Optional helper: release ALL active holds for a user (returns row count)
    public int releaseAllActiveHoldsForUser(long userId) {
        return jdbc.update(
                "UPDATE legal_holds SET released_at = now() " +
                        "WHERE user_id = ? AND released_at IS NULL",
                userId
        );
    }

    public List<Map<String, Object>> listActiveHoldsForUser(long userId) {
        return jdbc.queryForList(
                "SELECT hold_id, user_id, hold_reason, created_at " +
                        "FROM legal_holds WHERE user_id = ? AND released_at IS NULL " +
                        "ORDER BY created_at DESC",
                userId
        );
    }

    public List<Map<String, Object>> listAllActiveHolds() {
        return jdbc.queryForList(
                "SELECT hold_id, user_id, hold_reason, created_at " +
                        "FROM legal_holds WHERE released_at IS NULL " +
                        "ORDER BY created_at DESC"
        );
    }

    // ----------------------------
    // Audit
    // ----------------------------
    public List<Map<String, Object>> getDeletionAudit(int limit) {
        return jdbc.queryForList(
                "SELECT * FROM deletion_audit ORDER BY created_at DESC LIMIT ?",
                limit
        );
    }

    public List<Map<String, Object>> getBackupProtectionAudit(int limit) {
        return jdbc.queryForList(
                "SELECT * FROM backup_protection_audit ORDER BY created_at DESC LIMIT ?",
                limit
        );
    }
}
