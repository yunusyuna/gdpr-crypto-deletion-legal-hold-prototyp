async function apiJson(path, options = {}) {
  const res = await fetch(path, {
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });

  const text = await res.text();
  let data;
  try { data = text ? JSON.parse(text) : null; }
  catch { data = { raw: text }; }

  if (!res.ok) {
    const err = new Error(`HTTP ${res.status}`);
    err.payload = data;
    throw err;
  }
  return data;
}

function setPre(id, obj) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = typeof obj === "string" ? obj : JSON.stringify(obj, null, 2);
}

function activeNav() {
  const here = location.pathname.split("/").pop() || "index.html";
  document.querySelectorAll(".nav a").forEach(a => {
    const target = a.getAttribute("href").split("/").pop();
    a.classList.toggle("active", target === here);
  });
}

function getGlobalUserId() {
  const el = document.getElementById("globalUserId");
  const v = el ? el.value.trim() : "";
  return v ? Number(v) : null;
}

window.addEventListener("DOMContentLoaded", activeNav);
