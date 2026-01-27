package edu.depaul.grap.api;

import edu.depaul.grap.dao.GrapDao;
import edu.depaul.grap.dto.ApiResponse;
import edu.depaul.grap.dto.CreateUserRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class GrapController {

    private final GrapDao dao;

    public GrapController(GrapDao dao) {
        this.dao = dao;
    }

    // ----------------------------
    // Health
    // ----------------------------
    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("pong");
    }

    // ----------------------------
    // Users
    // ----------------------------
    @PostMapping("/users")
    public ApiResponse<?> createUser(@RequestBody CreateUserRequest req) {
        if (req == null || req.full_name == null || req.full_name.isBlank()
                || req.email == null || req.email.isBlank()) {
            return ApiResponse.err("full_name and email are required", null);
        }

        try {
            var row = dao.createUser(req.full_name.trim(), req.email.trim(), req.phone);
            return ApiResponse.ok(row);
        } catch (DataAccessException e) {
            return ApiResponse.err("Failed to create user", e.getMostSpecificCause().getMessage());
        }
    }

    // GET /api/users?limit=50
    @GetMapping("/users")
    public ApiResponse<?> listUsers(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        try {
            int safe = Math.max(1, Math.min(limit, 500));
            return ApiResponse.ok(Map.of("limit", safe, "rows", dao.listUsers(safe)));
        } catch (DataAccessException e) {
            return ApiResponse.err("List users failed", e.getMostSpecificCause().getMessage());
        }
    }

    // GET /api/users/{id}
    @GetMapping("/users/{id}")
    public ApiResponse<?> getUser(@PathVariable("id") long id) {
        try {
            return ApiResponse.ok(dao.getUser(id));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return ApiResponse.err("User not found", null);
        } catch (DataAccessException e) {
            return ApiResponse.err("Get user failed", e.getMostSpecificCause().getMessage());
        }
    }

    // GET /api/users/{id}/keys  (shows key_store rows)
    @GetMapping("/users/{id}/keys")
    public ApiResponse<?> getUserKeys(@PathVariable("id") long id) {
        try {
            return ApiResponse.ok(Map.of("user_id", id, "keys", dao.getUserKeys(id)));
        } catch (DataAccessException e) {
            return ApiResponse.err("Get user keys failed", e.getMostSpecificCause().getMessage());
        }
    }

    // Destroy keys (guarded if function exists; otherwise swap to destroyUserKeys)
    @PostMapping("/users/{id}/destroy-keys")
    public ApiResponse<?> destroyKeys(@PathVariable("id") long id) {
        try {
            // If you did NOT create destroy_user_keys_guarded in SQL, change to dao.destroyUserKeys(id)
            int destroyed = dao.destroyUserKeysGuarded(id);
            return ApiResponse.ok(Map.of("user_id", id, "keys_destroyed", destroyed));
        } catch (DataAccessException e) {
            return ApiResponse.err("Destroy keys failed", e.getMostSpecificCause().getMessage());
        }
    }

    // ----------------------------
    // Admin: truncate + restore
    // ----------------------------
    @PostMapping("/admin/truncate-users")
    public ApiResponse<?> truncateUsers() {
        try {
            dao.truncateUsersCascade();
            return ApiResponse.ok(Map.of("ok", true));
        } catch (DataAccessException e) {
            return ApiResponse.err("Truncate users failed", e.getMostSpecificCause().getMessage());
        }
    }

    @PostMapping("/admin/restore-users")
    public ApiResponse<?> restoreUsers() {
        try {
            long rows = dao.restoreUsersFromShadow();
            return ApiResponse.ok(Map.of("rows_restored", rows));
        } catch (DataAccessException e) {
            return ApiResponse.err("Restore failed", e.getMostSpecificCause().getMessage());
        }
    }

    // ----------------------------
    // Phase 4: Backups
    // ----------------------------
    @PostMapping("/admin/backups")
    public ApiResponse<?> createBackup(@RequestBody edu.depaul.grap.dto.CreateBackupRequest req) {
        try {
            // DB CHECK expects FULL/INCR. Default to INCR.
            String type = (req != null && req.backup_type != null && !req.backup_type.isBlank())
                    ? req.backup_type.trim().toUpperCase()
                    : "INCR";

            boolean prot = (req != null) && req.protected_by_hold;

            var row = dao.createBackupRun(type, prot);
            return ApiResponse.ok(row);
        } catch (DataAccessException e) {
            return ApiResponse.err("Create backup failed", e.getMostSpecificCause().getMessage());
        }
    }

    @PostMapping("/admin/backups/{backupId}/users")
    public ApiResponse<?> addUserToBackup(@PathVariable long backupId,
                                          @RequestBody edu.depaul.grap.dto.AddUserToBackupRequest req) {
        if (req == null || req.user_id <= 0) {
            return ApiResponse.err("user_id is required", null);
        }
        try {
            var row = dao.addUserToBackup(backupId, req.user_id);
            return ApiResponse.ok(row);
        } catch (DataAccessException e) {
            return ApiResponse.err("Add user to backup failed", e.getMostSpecificCause().getMessage());
        }
    }

    // GET /api/admin/backups
    @GetMapping("/admin/backups")
    public ApiResponse<?> listBackups() {
        try {
            return ApiResponse.ok(dao.listBackups());
        } catch (DataAccessException e) {
            return ApiResponse.err("List backups failed", e.getMostSpecificCause().getMessage());
        }
    }

    // GET /api/admin/users/{id}/backups
    @GetMapping("/admin/users/{id}/backups")
    public ApiResponse<?> listBackupsForUser(@PathVariable("id") long id) {
        try {
            return ApiResponse.ok(Map.of("user_id", id, "backups", dao.listBackupsForUser(id)));
        } catch (DataAccessException e) {
            return ApiResponse.err("List user backups failed", e.getMostSpecificCause().getMessage());
        }
    }

    // ----------------------------
    // Holds (FIXED: legal_holds has hold_reason + created_at, no case_id)
    // ----------------------------

    // POST /api/admin/users/{id}/holds   body: { "hold_reason": "text..." }
    @PostMapping("/admin/users/{id}/holds")
    public ApiResponse<?> placeHold(@PathVariable("id") long id,
                                    @RequestBody edu.depaul.grap.dto.PlaceHoldRequest req) {
        // We'll read either req.hold_reason OR fallback to req.reason if your DTO still has "reason"
        String holdReason = null;
        if (req != null) {
            // try both field names to be robust with your current DTO
            try {
                holdReason = (String) req.getClass().getField("hold_reason").get(req);
            } catch (Exception ignored) { }
            if (holdReason == null) {
                try {
                    holdReason = (String) req.getClass().getField("reason").get(req);
                } catch (Exception ignored) { }
            }
        }

        if (holdReason == null || holdReason.isBlank()) {
            return ApiResponse.err("hold_reason is required", null);
        }

        try {
            var row = dao.placeLegalHold(id, holdReason.trim());
            return ApiResponse.ok(row);
        } catch (DataAccessException e) {
            return ApiResponse.err("Place legal hold failed", e.getMostSpecificCause().getMessage());
        }
    }

    // POST /api/admin/holds/{holdId}/release
    @PostMapping("/admin/holds/{holdId}/release")
    public ApiResponse<?> releaseHoldByHoldId(@PathVariable("holdId") long holdId) {
        try {
            var row = dao.releaseLegalHoldByHoldId(holdId);
            return ApiResponse.ok(row);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return ApiResponse.err("No active hold found for this hold_id", null);
        } catch (DataAccessException e) {
            return ApiResponse.err("Release legal hold failed", e.getMostSpecificCause().getMessage());
        }
    }

    // GET /api/admin/users/{id}/holds   (active holds for a user)
    @GetMapping("/admin/users/{id}/holds")
    public ApiResponse<?> listHoldsForUser(@PathVariable("id") long id) {
        try {
            return ApiResponse.ok(Map.of("user_id", id, "active_holds", dao.listActiveHoldsForUser(id)));
        } catch (DataAccessException e) {
            return ApiResponse.err("List holds failed", e.getMostSpecificCause().getMessage());
        }
    }

    // GET /api/admin/holds   (all active holds)
    @GetMapping("/admin/holds")
    public ApiResponse<?> listAllActiveHolds() {
        try {
            return ApiResponse.ok(Map.of("active_holds", dao.listAllActiveHolds()));
        } catch (DataAccessException e) {
            return ApiResponse.err("List holds failed", e.getMostSpecificCause().getMessage());
        }
    }

    // ----------------------------
    // Audit
    // ----------------------------
    @GetMapping("/admin/audit/deletion")
    public ApiResponse<?> deletionAudit(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        try {
            int safe = Math.max(1, Math.min(limit, 500));
            var rows = dao.getDeletionAudit(safe);
            return ApiResponse.ok(Map.of("limit", safe, "rows", rows));
        } catch (DataAccessException e) {
            return ApiResponse.err("Fetch deletion audit failed", e.getMostSpecificCause().getMessage());
        }
    }

    @GetMapping("/admin/audit/backup-protection")
    public ApiResponse<?> backupProtectionAudit(@RequestParam(value = "limit", defaultValue = "50") int limit) {
        try {
            int safe = Math.max(1, Math.min(limit, 500));
            var rows = dao.getBackupProtectionAudit(safe);
            return ApiResponse.ok(Map.of("limit", safe, "rows", rows));
        } catch (DataAccessException e) {
            return ApiResponse.err("Fetch backup protection audit failed", e.getMostSpecificCause().getMessage());
        }
    }
}
