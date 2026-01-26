package edu.depaul.grap.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
public class GrapDao {
    private final JdbcTemplate jdbc;

    public GrapDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> createUser(String fullName, String email, String phone) {
        return jdbc.queryForMap(
                "INSERT INTO users(full_name, email, phone) VALUES (?,?,?) RETURNING user_id, created_at",
                fullName, email, phone
        );
    }

    public long destroyUserKeys(long userId) {
        return jdbc.queryForObject("SELECT destroy_user_keys(?)", Long.class, userId);
    }

    public long restoreUsersFromShadow() {
        return jdbc.queryForObject("SELECT restore_users_from_shadow()", Long.class);
    }

    public void truncateUsersCascade() {
        jdbc.execute("TRUNCATE users RESTART IDENTITY CASCADE");
    }
    public java.util.Map<String, Object> destroyUserKeysGuarded(long userId) {
    return jdbc.queryForMap("SELECT * FROM destroy_user_keys_guarded(?)", userId);
    }
public java.util.Map<String, Object> createBackupRun(String backupType, boolean protectedByHold) {
    return jdbc.queryForMap(
            "INSERT INTO backup_runs(backup_type, protected_by_hold) VALUES (?, ?) RETURNING backup_id, backup_type, protected_by_hold, run_at",
            backupType, protectedByHold
    );
}

public java.util.Map<String, Object> addUserToBackup(long backupId, long userId) {
    return jdbc.queryForMap(
            "INSERT INTO backup_user_index(backup_id, user_id) VALUES (?, ?) RETURNING backup_id, user_id",
            backupId, userId
    );
}
public java.util.Map<String, Object> placeLegalHold(long userId, String caseId, String reason) {
    return jdbc.queryForMap(
        "INSERT INTO legal_holds(user_id, case_id, reason) VALUES (?,?,?) " +
        "RETURNING hold_id, user_id, case_id, reason, placed_at, released_at",
        userId, caseId, reason
    );
}

public java.util.Map<String, Object> releaseLegalHold(long userId, String caseId) {
    return jdbc.queryForMap(
        "UPDATE legal_holds " +
        "SET released_at = now() " +
        "WHERE user_id = ? AND case_id = ? AND released_at IS NULL " +
        "RETURNING hold_id, user_id, case_id, reason, placed_at, released_at",
        userId, caseId
    );
}

// Optional but very useful for UI + debugging
public java.util.List<java.util.Map<String, Object>> listActiveHolds(long userId) {
    return jdbc.queryForList(
        "SELECT hold_id, user_id, case_id, reason, placed_at " +
        "FROM legal_holds WHERE user_id = ? AND released_at IS NULL " +
        "ORDER BY placed_at DESC",
        userId
    );
}
public java.util.List<java.util.Map<String, Object>> getDeletionAudit(int limit) {
    return jdbc.queryForList(
        "SELECT * FROM deletion_audit ORDER BY created_at DESC LIMIT ?",
        limit
    );
}

public java.util.List<java.util.Map<String, Object>> getBackupProtectionAudit(int limit) {
    return jdbc.queryForList(
        "SELECT * FROM backup_protection_audit ORDER BY created_at DESC LIMIT ?",
        limit
    );
}

}
