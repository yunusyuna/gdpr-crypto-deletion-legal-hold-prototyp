package edu.depaul.grap.api;

import edu.depaul.grap.dao.GrapDao;
import edu.depaul.grap.dto.ApiResponse;
import edu.depaul.grap.dto.CreateUserRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class GrapController {

    private final GrapDao dao;

    public GrapController(GrapDao dao) {
        this.dao = dao;
    }

    @GetMapping("/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.ok("pong");
    }

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

   // @PostMapping("/users/{id}/destroy-keys")
    //public ApiResponse<?> destroyKeys(@PathVariable("id") long id) {
      //  try {
        //    long keysDestroyed = dao.destroyUserKeys(id);
          //  return ApiResponse.ok(java.util.Map.of(
            //        "user_id", id,
            //        "keys_destroyed", keysDestroyed
            // ));
        // } catch (DataAccessException e) {
           // return ApiResponse.err("Destroy keys failed", e.getMostSpecificCause().getMessage());
        // }
   // }
@PostMapping("/users/{id}/destroy-keys")
public ApiResponse<?> destroyKeys(@PathVariable("id") long id) {
    try {
        var row = dao.destroyUserKeysGuarded(id);
        boolean allowed = (Boolean) row.get("allowed");

        if (!allowed) {
            return ApiResponse.err((String) row.get("reason"),
                    "Deletion blocked due to legal hold / protected backup");
        }

        return ApiResponse.ok(row);
    } catch (DataAccessException e) {
        return ApiResponse.err("Destroy keys failed", e.getMostSpecificCause().getMessage());
    }
}

    @PostMapping("/admin/truncate-users")
    public ApiResponse<?> truncateUsers() {
        try {
            dao.truncateUsersCascade();
            return ApiResponse.ok(java.util.Map.of("ok", true));
        } catch (DataAccessException e) {
            return ApiResponse.err("Truncate users failed", e.getMostSpecificCause().getMessage());
        }
    }

    @PostMapping("/admin/restore-users")
    public ApiResponse<?> restoreUsers() {
        try {
            long rows = dao.restoreUsersFromShadow();
            return ApiResponse.ok(java.util.Map.of("rows_restored", rows));
        } catch (DataAccessException e) {
            return ApiResponse.err("Restore failed", e.getMostSpecificCause().getMessage());
        }
    }
    @PostMapping("/admin/backups")
public ApiResponse<?> createBackup(@RequestBody edu.depaul.grap.dto.CreateBackupRequest req) {
    try {
        String type = (req != null && req.backup_type != null && !req.backup_type.isBlank())
                ? req.backup_type.trim()
                : "daily";
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
@PostMapping("/admin/users/{id}/holds")
public ApiResponse<?> placeHold(@PathVariable("id") long id,
                                @RequestBody edu.depaul.grap.dto.PlaceHoldRequest req) {
    if (req == null || req.case_id == null || req.case_id.isBlank()) {
        return ApiResponse.err("case_id is required", null);
    }
    String reason = (req.reason == null) ? null : req.reason.trim();

    try {
        var row = dao.placeLegalHold(id, req.case_id.trim(), reason);
        return ApiResponse.ok(row);
    } catch (org.springframework.dao.DataAccessException e) {
        return ApiResponse.err("Place legal hold failed", e.getMostSpecificCause().getMessage());
    }
}

@PostMapping("/admin/users/{id}/holds/release")
public ApiResponse<?> releaseHold(@PathVariable("id") long id,
                                  @RequestBody edu.depaul.grap.dto.ReleaseHoldRequest req) {
    if (req == null || req.case_id == null || req.case_id.isBlank()) {
        return ApiResponse.err("case_id is required", null);
    }
    try {
        var row = dao.releaseLegalHold(id, req.case_id.trim());
        return ApiResponse.ok(row);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
        return ApiResponse.err("No active hold found for this user + case_id", null);
    } catch (org.springframework.dao.DataAccessException e) {
        return ApiResponse.err("Release legal hold failed", e.getMostSpecificCause().getMessage());
    }
}

// Optional (recommended for your multipage UI)
@GetMapping("/admin/users/{id}/holds")
public ApiResponse<?> listHolds(@PathVariable("id") long id) {
    try {
        var rows = dao.listActiveHolds(id);
        return ApiResponse.ok(java.util.Map.of("user_id", id, "active_holds", rows));
    } catch (org.springframework.dao.DataAccessException e) {
        return ApiResponse.err("List holds failed", e.getMostSpecificCause().getMessage());
    }
}

@GetMapping("/admin/audit/deletion")
public ApiResponse<?> deletionAudit(@RequestParam(value="limit", defaultValue="50") int limit) {
    try {
        int safe = Math.max(1, Math.min(limit, 500));
        var rows = dao.getDeletionAudit(safe);
        return ApiResponse.ok(java.util.Map.of("limit", safe, "rows", rows));
    } catch (org.springframework.dao.DataAccessException e) {
        return ApiResponse.err("Fetch deletion audit failed", e.getMostSpecificCause().getMessage());
    }
}

@GetMapping("/admin/audit/backup-protection")
public ApiResponse<?> backupProtectionAudit(@RequestParam(value="limit", defaultValue="50") int limit) {
    try {
        int safe = Math.max(1, Math.min(limit, 500));
        var rows = dao.getBackupProtectionAudit(safe);
        return ApiResponse.ok(java.util.Map.of("limit", safe, "rows", rows));
    } catch (org.springframework.dao.DataAccessException e) {
        return ApiResponse.err("Fetch backup protection audit failed", e.getMostSpecificCause().getMessage());
    }
}

}

