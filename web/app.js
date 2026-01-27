const $ = (id) => document.getElementById(id);

const auditRows = [];

function addAudit(action, userId, detail) {
  const now = new Date().toISOString();
  auditRows.unshift({ now, action, userId, detail });
  renderAudit();
}

function renderAudit() {
  const tbody = $("auditTable").querySelector("tbody");
  tbody.innerHTML = "";
  if (auditRows.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="muted">No audit entries yet.</td></tr>`;
    return;
  }
  for (const r of auditRows.slice(0, 12)) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${r.now}</td>
      <td>${r.action}</td>
      <td>${r.userId ?? "-"}</td>
      <td>${r.detail}</td>
    `;
    tbody.appendChild(tr);
  }
}

function setPanel(id, obj) {
  $(id).textContent = typeof obj === "string" ? obj : JSON.stringify(obj, null, 2);
}

// --- Form handlers ---
$("createUserForm").addEventListener("submit", (e) => {
  e.preventDefault();
  const payload = {
    full_name: $("fullName").value.trim(),
    email: $("email").value.trim(),
    phone: $("phone").value.trim(),
  };

  // TODO: replace with fetch('/api/users', {method:'POST', body: JSON.stringify(payload)})
  const simulatedUserId = Math.floor(Math.random() * 900) + 1;

  setPanel("activeUser", { user_id: simulatedUserId, ...payload, table: "users" });
  setPanel("shadowUser", { user_id: simulatedUserId, email_enc: "<ciphertext>", table: "users_shadow" });
  setPanel("keys", [
    { purpose: "name", destroyed_at: null },
    { purpose: "email", destroyed_at: null },
    { purpose: "phone", destroyed_at: null },
  ]);

  addAudit("CREATE_USER", simulatedUserId, "Inserted into users; trigger would write users_shadow + key_store.");
  e.target.reset();
});

$("btnDestroyKeys").addEventListener("click", () => {
  const uid = Number($("eraseUserId").value || 1);
  // TODO: call /api/erase/:userId
  addAudit("CRYPTO_ERASE", uid, "Would call destroy_user_keys(user_id) and write deletion_audit.");
  setPanel("keys", { user_id: uid, status: "DESTROYED (simulated)" });
});

$("btnRestoreSim").addEventListener("click", () => {
  addAudit("RESTORE_SIM", "-", "Would truncate users and run restore_users_from_shadow().");
  setPanel("activeUser", "Restore complete (simulated). Redacted rows shown for deleted users.");
});

$("btnAddHold").addEventListener("click", () => {
  const uid = Number($("holdUserId").value || 1);
  const reason = $("holdReason").value.trim() || "Court Order";
  addAudit("ADD_LEGAL_HOLD", uid, `Hold active: ${reason}`);
});

$("btnReleaseHold").addEventListener("click", () => {
  const uid = Number($("holdUserId").value || 1);
  addAudit("RELEASE_HOLD", uid, "Released legal hold.");
});

$("btnProtectBackups").addEventListener("click", () => {
  const uid = Number($("holdUserId").value || 1);
  addAudit("PROTECT_BACKUPS", uid, "Would call protect_backups_for_legal_hold(user_id).");
  setPanel("backups", [
    { backup_id: 1, protected_by_hold: true },
    { backup_id: 2, protected_by_hold: false },
  ]);
});

$("btnLookupBackups").addEventListener("click", () => {
  const uid = Number($("holdUserId").value || 1);
  addAudit("LOOKUP_BACKUPS", uid, "Would query backup_user_index to list impacted backups.");
  setPanel("backups", [{ backup_id: 1, backup_type: "FULL" }]);
});

$("btnRefreshUser").addEventListener("click", () => {
  addAudit("REFRESH_STATUS", "-", "Would refresh active/shadow/keys panels from API.");
});

$("btnRefreshKeys").addEventListener("click", () => {
  addAudit("REFRESH_KEYS", "-", "Would refresh key_store state from API.");
});

$("btnLoadLocalCSVs").addEventListener("click", () => {
  // Demo-only: this shows intended UI values
  $("mLatency").textContent = "ON: 4.2ms vs OFF: 1.1ms (example)";
  $("mStorage").textContent = "shadow 2.3× users (example)";
  $("mRestore").textContent = "1000 restored / 300 redacted (example)";
  addAudit("LOAD_METRICS", "-", "Would load CSV metrics and show charts.");
});

// initial render
renderAudit();
