# Methodology

## 1. Research Problem

Modern data protection regulations such as GDPR and CCPA require organizations to permanently delete personal data upon request (e.g., GDPR Article 17 – Right to be Forgotten).  
However, traditional database deletion mechanisms are insufficient because:

- Deleting a row from a live database does not remove its presence from backups.
- Database backups are immutable and cannot be selectively edited.
- Organizations must simultaneously preserve data under legal hold during litigation.

This creates a conflict between **privacy compliance** and **legal preservation** that existing database systems do not adequately address.

---

## 2. Research Objective

The objective of this project is to design and evaluate a database-level mechanism that:

1. Enables **selective, user-level deletion** of personal data
2. Ensures deletion applies to **all historical backups**
3. Does **not require modifying backup files**
4. Supports **legal holds** by identifying which backups contain relevant data
5. Introduces minimal performance and storage overhead

---

## 3. High-Level Approach

This prototype adopts **selective cryptographic deletion** as its core technique.

Instead of deleting data from backups, the system:
- Encrypts sensitive data before it reaches backup storage
- Stores encryption keys separately from backups
- Permanently deletes data by destroying encryption keys

Once a key is destroyed, all corresponding encrypted data becomes computationally unrecoverable, even if present in backups.

---

## 4. System Design Overview

The system consists of four primary layers:

### 4.1 Active Tables (Plaintext Layer)
- Used by the application during normal operation
- Store data in plaintext for query efficiency
- Subject to encryption triggers

### 4.2 Shadow Tables (Encrypted Layer)
- Store encrypted copies of sensitive fields
- Included in database backups
- Contain no plaintext personal data

### 4.3 Key Management Layer
- Stores encryption keys used for data protection
- Keys are scoped per user and data purpose
- Keys are excluded from backups
- Destroying a key constitutes cryptographic deletion

### 4.4 Backup Metadata Catalog
- Tracks which users and records appear in which backups
- Supports legal hold discovery
- Avoids restoring full backups during investigations

---

## 5. Encryption Strategy

### 5.1 Encryption Granularity

Encryption is applied at the **column level**, targeting only sensitive attributes such as:
- Email addresses
- Phone numbers
- Physical addresses

This avoids the performance cost of full-database encryption while still providing privacy guarantees.

### 5.2 Key Scoping

Keys are scoped as:

- **Per user**
- **Per data purpose** (e.g., email, phone)
- Optionally bucketed by time (e.g., daily)

This design enables:
- Fine-grained deletion
- Limited blast radius when a key is destroyed
- Better performance trade-offs

---

## 6. Automation via Triggers

Database triggers ensure transparency and correctness:

- On INSERT or UPDATE of active tables:
  - Sensitive fields are encrypted
  - Encrypted values are written to shadow tables
  - Key identifiers are recorded

Applications are unaware of this process, ensuring compatibility with existing systems.

---

## 7. Cryptographic Deletion Process

When a user requests deletion:

1. All keys associated with that user are identified
2. Keys are destroyed or rendered unusable
3. Encrypted data remains in shadow tables and backups
4. Without keys, encrypted data is permanently unreadable

This guarantees deletion across:
- Live database
- Full backups
- Incremental backups
- Archived storage

No backup rewriting is required.

---

## 8. Restore Semantics

During database restore:

- Shadow tables are restored from backup
- The system attempts to decrypt encrypted fields
- If the corresponding key exists, decryption succeeds
- If the key has been destroyed:
  - The field is restored as NULL or redacted
  - Or the entire row is omitted, depending on policy

This ensures that deleted data cannot reappear after restore.

---

## 9. Legal Hold Support

Legal holds require preserving data during litigation.

This system supports legal holds by:

- Maintaining a change log of user-related operations
- Recording backup time ranges
- Identifying which backups contain data for specific users or cases

Before key destruction:
- The system checks whether a legal hold is active
- Keys associated with held users are preserved
- Deletion is deferred until the hold is released

---

## 10. Experimental Methodology

The prototype is evaluated using controlled experiments measuring:

- Encryption and trigger overhead
- Storage overhead from shadow tables
- Restore time with and without deleted keys
- Accuracy of legal-hold backup lookup
- Scalability with increasing data volume

Experiments are repeatable and automated using Python scripts.

---

## 11. Threat Model and Assumptions

The system assumes:
- Adversaries cannot access encryption keys once destroyed
- Backup storage may be compromised
- Database administrators follow key-management policies

The system does not protect against:
- Keys exfiltrated before deletion
- Malicious insiders retaining key copies
- Compromised application logic

These limitations align with prior cryptographic erasure research.

---

## 12. Expected Outcomes

This methodology demonstrates that:

- Selective cryptographic deletion is feasible at the database layer
- Backups can be rendered privacy-compliant without modification
- Legal holds can coexist with privacy requirements
- Lightweight metadata structures can support discovery efficiently

---

## 13. Reproducibility

All schema definitions, triggers, scripts, and experiments are included in this repository to ensure reproducibility and transparency.
