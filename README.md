# GDPR Cryptographic Deletion & Legal Hold Prototype

## Overview

Modern privacy regulations such as **GDPR** and **CCPA** require organizations to permanently delete personal data upon request (e.g., *Right to be Forgotten*).  
At the same time, organizations are legally required to **preserve data under legal hold** when litigation or investigations are anticipated.

This creates a fundamental conflict:

-  Deleting data from a live database does **not** remove it from backups.
-  Modifying or rewriting historical backups is impractical and unsafe.
-  Legal holds require selective preservation without disrupting operations.

This repository implements a **research prototype** that resolves this conflict using:

- **Selective cryptographic deletion** (a.k.a. cryptographic erasure / crypto-shredding)
- **Policy-driven encryption using PostgreSQL**
- **Lightweight backup lookup mechanisms for legal holds**

The system allows **true deletion from backups without modifying backups**, while still supporting **legal hold discovery**.

---

## Core Idea

Instead of deleting data from backups, this prototype:

1. Encrypts sensitive data using **user-specific symmetric encryption keys**
2. Stores encrypted data in **shadow tables** that are included in backups
3. Stores encryption keys **outside of backups**
4. Permanently deletes data by **destroying encryption keys**
5. Supports legal holds by identifying which backups contain relevant data

Once a key is destroyed, encrypted data becomes **cryptographically unrecoverable**, even if it exists in old backups.
