package edu.depaul.grap.dto;

public class CreateBackupRequest {
    public String backup_type;          // e.g., "daily"
    public boolean protected_by_hold;   // true/false
}
