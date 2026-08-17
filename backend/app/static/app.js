/**
 * VocalBharat — AI Hindi to English Spoken Tutor Web Application
 * Core Client Application Controller
 */

// ── State Management ────────────────────────────────────────────────────────
const state = {
  token: localStorage.getItem("vb_auth_token") || null,
  userEmail: localStorage.getItem("vb_user_email") || null,
  activeView: "tutor",
  tutorMode: "voice",
  
  // Media Recording
  isRecording: false,
  mediaRecorder: null,
  audioChunks: [],
  audioStream: null,
  recordStartTime: null,
  timerInterval: null,
  audioContext: null,
  analyser: null,
  animFrameId: null,

  // Audio Playback
  currentAudioB64: null,
  audioElement: null,
  isPlayingAudio: false,

  // Subscription Status
  subscription: {
    isActive: false,
    hasSubscription: false,
    plan: null,
    requestsUsed: 0,
    requestsLimit: 20,
    requestsRemaining: 20,
    daysRemaining: 0,
    quotaExceeded: false,
  },

  // Gateway key
  razorpayKeyId: "rzp_test_TQNC46en6BBlI1",
};

// ── Initialization ──────────────────────────────────────────────────────────
document.addEventListener("DOMContentLoaded", async () => {
  console.log("VocalBharat Web App initializing...");
  
  // 1. Initial health ping
  checkBackendHealth();
  setInterval(checkBackendHealth, 30000);

  // 2. Fetch payment config & Razorpay Key ID
  fetchPaymentConfig();

  // 3. Check auth & subscription status
  if (state.token) {
    await loadUserProfile();
  } else {
    updateAccountUI();
  }
  await loadSubscriptionStatus();

  // 4. Preload default grammar topic
  loadGrammarTopic("Tenses");
});

// ── Navigation ──────────────────────────────────────────────────────────────
function navigateTo(viewName) {
  state.activeView = viewName;

  // Update tabs
  document.querySelectorAll(".nav-tab").forEach(tab => tab.classList.remove("active"));
  const activeTab = document.getElementById(`tab-${viewName}`);
  if (activeTab) activeTab.classList.add("active");

  // Update views
  document.querySelectorAll(".view-section").forEach(view => view.classList.remove("active"));
  const targetView = document.getElementById(`view-${viewName}`);
  if (targetView) targetView.classList.add("active");

  window.scrollTo({ top: 0, behavior: "smooth" });
}

// ── Backend Health Check ────────────────────────────────────────────────────
async function checkBackendHealth() {
  const dot = document.getElementById("statusDot");
  const text = document.getElementById("statusText");
  const startTime = Date.now();

  try {
    const res = await fetch("/health", { method: "GET" });
    if (res.ok) {
      const latency = Date.now() - startTime;
      dot.className = "status-indicator-dot online";
      text.textContent = `Online (${latency}ms)`;
    } else {
      dot.className = "status-indicator-dot offline";
      text.textContent = "Server Issue";
    }
  } catch (err) {
    dot.className = "status-indicator-dot offline";
    text.textContent = "Offline";
  }
}

async function fetchPaymentConfig() {
  try {
    const res = await fetch("/api/payment-config");
    if (res.ok) {
      const data = await res.json();
      if (data.key_id) {
        state.razorpayKeyId = data.key_id;
      }
    }
  } catch (_) {}
}

// ── Mode Switcher (Voice vs Text) ───────────────────────────────────────────
function setTutorMode(mode) {
  state.tutorMode = mode;
  document.getElementById("modeBtnVoice").classList.toggle("active", mode === "voice");
  document.getElementById("modeBtnText").classList.toggle("active", mode === "text");
  document.getElementById("voicePanel").classList.toggle("active", mode === "voice");
  document.getElementById("textPanel").classList.toggle("active", mode === "text");
}

function applyPrompt(text) {
  document.getElementById("textInputHindi").value = text;
  document.getElementById("textInputHindi").focus();
}

// ── Voice Recording & Audio Visualizer ──────────────────────────────────────
async function toggleRecording() {
  if (state.isRecording) {
    stopRecording();
  } else {
    startRecording();
  }
}

async function startRecording() {
  if (state.subscription.quotaExceeded && !state.subscription.isActive) {
    showToast("20 Free sessions used. Please upgrade to Pro!", "error");
    navigateTo("pricing");
    return;
  }

  try {
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
    state.audioStream = stream;
    state.audioChunks = [];

    // Setup MediaRecorder
    let mimeType = "audio/webm;codecs=opus";
    if (!MediaRecorder.isTypeSupported(mimeType)) {
      mimeType = "audio/webm";
    }
    if (!MediaRecorder.isTypeSupported(mimeType)) {
      mimeType = ""; // Browser default
    }

    state.mediaRecorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream);

    state.mediaRecorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) {
        state.audioChunks.push(e.data);
      }
    };

    state.mediaRecorder.onstop = async () => {
      const audioBlob = new Blob(state.audioChunks, { type: "audio/webm" });
      await processAudioBlob(audioBlob);
    };

    state.mediaRecorder.start();
    state.isRecording = true;

    // UI Updates
    document.getElementById("micBtn").classList.add("recording");
    document.getElementById("micIcon").className = "fa-solid fa-stop";
    document.getElementById("micPulseRing").classList.add("recording");
    document.getElementById("micStatusLabel").textContent = "Listening... Tap to finish speaking";
    
    // Timer
    const timerEl = document.getElementById("recordingTimer");
    timerEl.classList.remove("hidden");
    state.recordStartTime = Date.now();
    state.timerInterval = setInterval(() => {
      const elapsedSec = Math.floor((Date.now() - state.recordStartTime) / 1000);
      const m = String(Math.floor(elapsedSec / 60)).padStart(2, "0");
      const s = String(elapsedSec % 60).padStart(2, "0");
      timerEl.textContent = `${m}:${s}`;
    }, 500);

    // Audio Visualizer
    setupAudioVisualizer(stream);

  } catch (err) {
    console.error("Microphone access error:", err);
    showToast("Microphone access denied or not available. Please allow mic permission.", "error");
  }
}

function stopRecording() {
  if (!state.isRecording) return;
  state.isRecording = false;

  if (state.mediaRecorder && state.mediaRecorder.state !== "inactive") {
    state.mediaRecorder.stop();
  }

  if (state.audioStream) {
    state.audioStream.getTracks().forEach(track => track.stop());
  }

  // Cleanup Timer & Visualizer
  clearInterval(state.timerInterval);
  document.getElementById("recordingTimer").classList.add("hidden");
  if (state.animFrameId) cancelAnimationFrame(state.animFrameId);

  // UI Updates
  document.getElementById("micBtn").classList.remove("recording");
  document.getElementById("micIcon").className = "fa-solid fa-microphone";
  document.getElementById("micPulseRing").classList.remove("recording");
  document.getElementById("micStatusLabel").textContent = "Processing speech...";
}

function setupAudioVisualizer(stream) {
  const canvas = document.getElementById("audioVisualizer");
  const ctx = canvas.getContext("2d");
  canvas.width = canvas.parentElement.clientWidth || 300;
  canvas.height = 60;

  try {
    const AudioCtx = window.AudioContext || window.webkitAudioContext;
    state.audioContext = new AudioCtx();
    const source = state.audioContext.createMediaStreamSource(stream);
    state.analyser = state.audioContext.createAnalyser();
    state.analyser.fftSize = 64;
    source.connect(state.analyser);

    const bufferLength = state.analyser.frequencyBinCount;
    const dataArray = new Uint8Array(bufferLength);

    function drawWave() {
      if (!state.isRecording) {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        return;
      }
      state.animFrameId = requestAnimationFrame(drawWave);
      state.analyser.getByteFrequencyData(dataArray);

      ctx.clearRect(0, 0, canvas.width, canvas.height);
      const barWidth = (canvas.width / bufferLength) * 1.5;
      let x = 0;

      for (let i = 0; i < bufferLength; i++) {
        const barHeight = (dataArray[i] / 255) * canvas.height;
        const gradient = ctx.createLinearGradient(0, canvas.height - barHeight, 0, canvas.height);
        gradient.addColorStop(0, "#00F5D4");
        gradient.addColorStop(1, "#2979FF");

        ctx.fillStyle = gradient;
        ctx.fillRect(x, canvas.height - barHeight, barWidth - 2, barHeight);
        x += barWidth;
      }
    }
    drawWave();
  } catch (e) {
    console.warn("Visualizer setup failed:", e);
  }
}

// ── Submit & Process Audio ──────────────────────────────────────────────────
async function processAudioBlob(blob) {
  showLoader("Transcribing speech with Whisper & analyzing with AI...");

  const formData = new FormData();
  formData.append("audio_file", blob, "recording.webm");

  const headers = {};
  if (state.token) {
    headers["Authorization"] = `Bearer ${state.token}`;
  }

  try {
    const res = await fetch("/tutor", {
      method: "POST",
      headers: headers,
      body: formData,
    });

    hideLoader();
    document.getElementById("micStatusLabel").textContent = "Tap microphone & speak in Hindi";

    if (!res.ok) {
      const err = await res.json().catch(() => ({ detail: "Request failed" }));
      if (res.status === 402 || (err.detail && err.detail.includes("quota"))) {
        showToast("Free usage limit reached. Please upgrade to Pro!", "error");
        navigateTo("pricing");
        return;
      }
      showToast(err.detail || "Translation failed. Please speak clearly.", "error");
      return;
    }

    const data = await res.json();
    renderTutorResults(data);
    loadSubscriptionStatus(); // Refresh quota count
  } catch (err) {
    hideLoader();
    document.getElementById("micStatusLabel").textContent = "Tap microphone & speak in Hindi";
    showToast("Network error communicating with tutor API.", "error");
  }
}

// ── Text-based Tutor Submission ─────────────────────────────────────────────
async function submitTextTutor() {
  const inputEl = document.getElementById("textInputHindi");
  const text = inputEl.value.trim();
  if (!text) {
    showToast("Please enter a Hindi or Hinglish sentence first.", "info");
    inputEl.focus();
    return;
  }

  if (state.subscription.quotaExceeded && !state.subscription.isActive) {
    showToast("20 Free sessions used. Please upgrade to Pro!", "error");
    navigateTo("pricing");
    return;
  }

  showLoader("Analyzing sentence and creating native English coaching...");

  const headers = { "Content-Type": "application/json" };
  if (state.token) {
    headers["Authorization"] = `Bearer ${state.token}`;
  }

  try {
    const res = await fetch("/tutor/text", {
      method: "POST",
      headers: headers,
      body: JSON.stringify({ text: text }),
    });

    hideLoader();

    if (!res.ok) {
      const err = await res.json().catch(() => ({ detail: "Request failed" }));
      if (res.status === 402 || (err.detail && err.detail.includes("quota"))) {
        showToast("Free usage limit reached. Please upgrade to Pro!", "error");
        navigateTo("pricing");
        return;
      }
      showToast(err.detail || "Coaching failed.", "error");
      return;
    }

    const data = await res.json();
    renderTutorResults(data);
    loadSubscriptionStatus();
  } catch (err) {
    hideLoader();
    showToast("Network error communicating with tutor API.", "error");
  }
}

// ── Render Tutor Results Card ───────────────────────────────────────────────
function renderTutorResults(data) {
  document.getElementById("tutorEmptyState").classList.add("hidden");
  const container = document.getElementById("tutorResultContainer");
  container.classList.remove("hidden");

  // 1. Hindi text
  document.getElementById("resHindiText").textContent = data.transcription || data.correction?.hindi_input || "--";

  // 2. English text
  document.getElementById("resEnglishText").textContent = data.correction?.english_translation || data.correction?.corrected || "--";

  // 3. Explanation
  document.getElementById("resExplanation").textContent = data.correction?.explanation || "No explanation needed.";

  // 4. Practice challenge
  document.getElementById("resPractice").textContent = data.correction?.practice || "Try saying: 'I am ready for the next challenge.'";

  // 5. Encouragement
  document.getElementById("resEncouragementText").textContent = data.correction?.encouragement || "Shabash! Great effort, keep learning!";

  // Audio Base64
  state.currentAudioB64 = data.audio_b64 || null;
  const audioBtn = document.getElementById("btnPlayAudio");
  if (state.currentAudioB64) {
    audioBtn.classList.remove("hidden");
    // Auto-play pronunciation once for instant feedback
    playPronunciationAudio(state.currentAudioB64);
  } else {
    audioBtn.classList.add("hidden");
  }
}

function resetTutorState() {
  document.getElementById("tutorResultContainer").classList.add("hidden");
  document.getElementById("tutorEmptyState").classList.remove("hidden");
  document.getElementById("textInputHindi").value = "";
  if (state.audioElement) {
    state.audioElement.pause();
  }
}

// ── Native Pronunciation Audio Playback ──────────────────────────────────────
function toggleAudioPlayback() {
  if (!state.currentAudioB64) return;
  if (state.isPlayingAudio && state.audioElement) {
    state.audioElement.pause();
    setAudioPlayingState(false);
  } else {
    playPronunciationAudio(state.currentAudioB64);
  }
}

function playPronunciationAudio(b64) {
  try {
    if (state.audioElement) {
      state.audioElement.pause();
    }
    state.audioElement = new Audio(`data:audio/mp3;base64,${b64}`);
    setAudioPlayingState(true);

    state.audioElement.onended = () => setAudioPlayingState(false);
    state.audioElement.onpause = () => setAudioPlayingState(false);
    state.audioElement.onerror = () => setAudioPlayingState(false);

    state.audioElement.play().catch(e => {
      console.warn("Audio autoplay blocked or failed:", e);
      setAudioPlayingState(false);
    });
  } catch (err) {
    console.error("Audio playback error:", err);
    setAudioPlayingState(false);
  }
}

function setAudioPlayingState(isPlaying) {
  state.isPlayingAudio = isPlaying;
  const icon = document.getElementById("audioPlayIcon");
  const label = document.getElementById("audioPlayLabel");
  if (isPlaying) {
    icon.className = "fa-solid fa-circle-pause";
    label.textContent = "Playing Audio...";
  } else {
    icon.className = "fa-solid fa-volume-high";
    label.textContent = "Listen Native Pronunciation";
  }
}

// ── Grammar Explorer ────────────────────────────────────────────────────────
async function loadGrammarTopic(topic) {
  // Update active chip
  document.querySelectorAll(".grammar-chip").forEach(chip => {
    chip.classList.toggle("active", chip.textContent.toLowerCase().includes(topic.toLowerCase().slice(0, 5)));
  });

  const loader = document.getElementById("grammarLoader");
  const details = document.getElementById("grammarDetails");
  loader.classList.remove("hidden");
  details.classList.add("hidden");

  try {
    const res = await fetch("/grammar/explain", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ topic: topic }),
    });

    loader.classList.add("hidden");
    details.classList.remove("hidden");

    if (!res.ok) {
      showToast("Could not load grammar explanation.", "error");
      return;
    }

    const data = await res.json();
    renderGrammarDetails(data);
  } catch (err) {
    loader.classList.add("hidden");
    showToast("Network error fetching grammar topic.", "error");
  }
}

function searchGrammarTopic() {
  const query = document.getElementById("grammarSearchInput").value.trim();
  if (!query) {
    showToast("Please enter a grammar topic to search.", "info");
    return;
  }
  loadGrammarTopic(query);
}

function renderGrammarDetails(data) {
  document.getElementById("gTopicTitle").textContent = data.topic || "Grammar Topic";
  document.getElementById("gTopicDifficulty").textContent = data.difficulty || "Intermediate";
  document.getElementById("gEnglishDef").textContent = data.definition || "--";
  document.getElementById("gHindiDef").textContent = data.hindi_definition || "--";

  // Examples Grid
  const examplesGrid = document.getElementById("gExamplesGrid");
  examplesGrid.innerHTML = "";
  if (data.examples && Array.isArray(data.examples)) {
    data.examples.forEach(ex => {
      const card = document.createElement("div");
      card.className = "example-item-card";
      card.innerHTML = `
        <div class="ex-english">${ex.english || ex.sentence || ""}</div>
        <div class="ex-hindi">${ex.hindi || ex.translation || ""}</div>
        <div class="ex-note">${ex.explanation || ex.note || ""}</div>
      `;
      examplesGrid.appendChild(card);
    });
  }

  // Tips & Mistakes List
  const tipsList = document.getElementById("gTipsList");
  tipsList.innerHTML = "";
  if (data.tips && Array.isArray(data.tips)) {
    data.tips.forEach(tip => {
      const item = document.createElement("div");
      item.className = "tip-item-card";
      item.innerHTML = `
        <i class="fa-solid fa-triangle-exclamation"></i>
        <p>${tip}</p>
      `;
      tipsList.appendChild(item);
    });
  }
}

// ── Authentication & User Session ───────────────────────────────────────────
function openAccountModal() {
  const modal = document.getElementById("accountModal");
  modal.classList.remove("hidden");
  if (state.token) {
    showAuthStep("authStepProfile");
  } else {
    showAuthStep("authStepEmail");
  }
}

function closeAccountModal() {
  document.getElementById("accountModal").classList.add("hidden");
}

function showAuthStep(stepId) {
  document.querySelectorAll(".auth-step").forEach(s => s.classList.remove("active"));
  document.getElementById(stepId).classList.add("active");
}

function resetAuthModal() {
  showAuthStep("authStepEmail");
}

let pendingAuthEmail = "";

async function requestOtp() {
  const emailInput = document.getElementById("authEmailInput");
  const email = emailInput.value.trim();
  if (!email) return;

  pendingAuthEmail = email;
  const btn = document.getElementById("btnRequestOtp");
  btn.innerHTML = `<span>Sending OTP...</span> <div class="spinner"></div>`;
  btn.disabled = true;

  try {
    const res = await fetch("/auth/request-otp", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: email }),
    });

    btn.innerHTML = `<span>Send 6-Digit OTP</span> <i class="fa-solid fa-paper-plane"></i>`;
    btn.disabled = false;

    if (!res.ok) {
      const err = await res.json().catch(() => ({ detail: "Failed to send OTP" }));
      showToast(err.detail || "Failed to send OTP", "error");
      return;
    }

    document.getElementById("authTargetEmail").textContent = email;
    showAuthStep("authStepOtp");
    document.getElementById("authOtpInput").focus();
    showToast(`OTP sent to ${email} (Check your console/logs if testing in dev)`, "info");
  } catch (err) {
    btn.innerHTML = `<span>Send 6-Digit OTP</span> <i class="fa-solid fa-paper-plane"></i>`;
    btn.disabled = false;
    showToast("Network error requesting OTP.", "error");
  }
}

async function verifyOtp() {
  const otpInput = document.getElementById("authOtpInput");
  const otp = otpInput.value.trim();
  if (!otp || otp.length !== 6) {
    showToast("Please enter the 6-digit OTP code.", "error");
    return;
  }

  const btn = document.getElementById("btnVerifyOtp");
  btn.innerHTML = `<span>Verifying...</span> <div class="spinner"></div>`;
  btn.disabled = true;

  try {
    const res = await fetch("/auth/verify-otp", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: pendingAuthEmail, otp: otp }),
    });

    btn.innerHTML = `<span>Verify & Log In</span> <i class="fa-solid fa-arrow-right"></i>`;
    btn.disabled = false;

    if (!res.ok) {
      const err = await res.json().catch(() => ({ detail: "Invalid OTP code" }));
      showToast(err.detail || "Invalid or expired OTP", "error");
      return;
    }

    const data = await res.json();
    state.token = data.access_token;
    state.userEmail = data.email;
    localStorage.setItem("vb_auth_token", data.access_token);
    localStorage.setItem("vb_user_email", data.email);

    showToast("Successfully signed in!", "success");
    await loadUserProfile();
    await loadSubscriptionStatus();
    closeAccountModal();
  } catch (err) {
    btn.innerHTML = `<span>Verify & Log In</span> <i class="fa-solid fa-arrow-right"></i>`;
    btn.disabled = false;
    showToast("Network error verifying OTP.", "error");
  }
}

async function loadUserProfile() {
  if (!state.token) return;

  try {
    const res = await fetch("/auth/me", {
      headers: { "Authorization": `Bearer ${state.token}` },
    });

    if (res.ok) {
      const profile = await res.json();
      state.userEmail = profile.email;
      updateAccountUI();
    } else {
      // Token expired
      logoutUser();
    }
  } catch (err) {
    updateAccountUI();
  }
}

function updateAccountUI() {
  const label = document.getElementById("accountLabel");
  const profileEmail = document.getElementById("profileUserEmail");
  const subEmail = document.getElementById("subUserEmail");

  if (state.token && state.userEmail) {
    label.textContent = state.userEmail.split("@")[0];
    profileEmail.textContent = state.userEmail;
    subEmail.textContent = state.userEmail;
  } else {
    label.textContent = "Sign In";
    profileEmail.textContent = "Guest";
    subEmail.textContent = "Guest Learner";
  }
}

function logoutUser() {
  state.token = null;
  state.userEmail = null;
  localStorage.removeItem("vb_auth_token");
  localStorage.removeItem("vb_user_email");
  updateAccountUI();
  loadSubscriptionStatus();
  closeAccountModal();
  showToast("You have been signed out.", "info");
}

// ── Subscription Status & Quota ─────────────────────────────────────────────
async function loadSubscriptionStatus() {
  const headers = {};
  if (state.token) {
    headers["Authorization"] = `Bearer ${state.token}`;
  }

  try {
    const res = await fetch("/subscription/status", { headers });
    if (res.ok) {
      const data = await res.json();
      state.subscription = {
        isActive: data.is_active || false,
        hasSubscription: data.has_subscription || false,
        plan: data.plan,
        requestsUsed: data.requests_used || 0,
        requestsLimit: data.requests_limit || 20,
        requestsRemaining: data.requests_remaining || 0,
        daysRemaining: data.days_remaining || 0,
        quotaExceeded: data.quota_exceeded || false,
      };
      renderSubscriptionUI();
    }
  } catch (err) {
    console.warn("Could not load subscription status:", err);
  }
}

function renderSubscriptionUI() {
  const sub = state.subscription;

  // Banner
  const banner = document.getElementById("quotaBanner");
  const quotaCount = document.getElementById("quotaCount");
  if (sub.isActive) {
    banner.style.display = "none";
  } else {
    banner.style.display = "flex";
    quotaCount.textContent = sub.requestsRemaining;
  }

  // Quota Nav Badge
  const badge = document.getElementById("quotaBadge");
  if (sub.isActive) {
    badge.textContent = `Pro (${sub.daysRemaining}d)`;
    badge.style.background = "rgba(0, 230, 118, 0.2)";
    badge.style.color = "#00E676";
  } else {
    badge.textContent = `${sub.requestsRemaining} Free`;
    badge.style.background = sub.quotaExceeded ? "rgba(255, 82, 82, 0.2)" : "rgba(0, 230, 118, 0.15)";
    badge.style.color = sub.quotaExceeded ? "#FF5252" : "#00E676";
  }

  // Status Card in Pricing View
  const subStatusText = document.getElementById("subStatusText");
  const leftLabel = document.getElementById("progressLabelLeft");
  const rightLabel = document.getElementById("progressLabelRight");
  const barFill = document.getElementById("progressBarFill");

  if (sub.isActive) {
    subStatusText.textContent = `⚡ Monthly Pro Active (${sub.daysRemaining} days left)`;
    leftLabel.textContent = `Unlimited AI Coaching Enabled`;
    rightLabel.textContent = `Active Plan`;
    barFill.style.width = `100%`;
  } else {
    subStatusText.textContent = `Free Starter Plan`;
    leftLabel.textContent = `Usage: ${sub.requestsUsed} / ${sub.requestsLimit} requests used`;
    rightLabel.textContent = `${sub.requestsRemaining} remaining`;
    const pct = Math.min(100, Math.round((sub.requestsUsed / sub.requestsLimit) * 100));
    barFill.style.width = `${pct}%`;
  }

  // Profile modal badges
  document.getElementById("profileRequestsCount").textContent = sub.isActive ? "Unlimited" : `${sub.requestsRemaining} left`;
  document.getElementById("profileSubStatus").textContent = sub.isActive ? `Monthly Pro (${sub.daysRemaining} days left)` : "Free Tier (20 limit)";
  document.getElementById("profilePlanBadge").textContent = sub.isActive ? "Active Plan: Monthly Pro" : "Active Plan: Free Tier";
}

// ── Razorpay Checkout Flow ──────────────────────────────────────────────────
async function startCheckoutFlow() {
  if (!state.token) {
    showToast("Please sign in with your email first to attach your subscription.", "info");
    openAccountModal();
    return;
  }

  const btn = document.getElementById("btnProCheckout");
  const spinner = document.getElementById("checkoutSpinner");
  const payText = document.getElementById("btnPayText");
  
  spinner.classList.remove("hidden");
  payText.textContent = "Preparing Gateway...";
  btn.disabled = true;

  try {
    // 1. Create order on backend
    const res = await fetch("/subscription/initiate", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${state.token}`,
      },
      body: JSON.stringify({ plan: "monthly" }),
    });

    spinner.classList.add("hidden");
    payText.textContent = "Upgrade to Monthly Pro (₹300)";
    btn.disabled = false;

    if (!res.ok) {
      const err = await res.json().catch(() => ({ detail: "Order initiation failed" }));
      showToast(err.detail || "Failed to create order", "error");
      return;
    }

    const orderOut = await res.json();
    openRazorpayModal(orderOut);

  } catch (err) {
    spinner.classList.add("hidden");
    payText.textContent = "Upgrade to Monthly Pro (₹300)";
    btn.disabled = false;
    showToast("Network error initiating payment.", "error");
  }
}

let orderPollingTimer = null;

function openRazorpayModal(orderOut) {
  if (typeof Razorpay === "undefined") {
    showToast("Razorpay SDK not loaded. Please check your internet connection.", "error");
    return;
  }

  const key = orderOut.gateway_key || state.razorpayKeyId || "rzp_test_TQNC46en6BBlI1";
  localStorage.setItem("vb_last_order_id", orderOut.order_id);

  // Start background auto-polling every 4 seconds for UPI QR scan payments
  startOrderPolling(orderOut.order_id);

  const options = {
    key: key,
    amount: (orderOut.amount || 300) * 100, // paise
    currency: orderOut.currency || "INR",
    name: "VocalBharat",
    description: "Monthly Pro Unlimited Pass",
    image: "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/1f399.png",
    order_id: orderOut.order_id,
    prefill: {
      email: state.userEmail || "",
    },
    theme: {
      color: "#2979FF",
    },
    handler: async function (response) {
      console.log("Razorpay payment success response:", response);
      stopOrderPolling();
      await confirmPaymentOnServer({
        order_id: response.razorpay_order_id || orderOut.order_id,
        payment_id: response.razorpay_payment_id,
        signature: response.razorpay_signature,
      });
    },
    modal: {
      ondismiss: function () {
        console.log("Razorpay modal dismissed. Checking if payment was completed in background...");
        // Do one quick check on dismiss just in case user completed UPI scan and closed window
        setTimeout(() => checkPaymentStatusManual(orderOut.order_id, false), 1500);
      },
    },
  };

  try {
    const rzp = new Razorpay(options);
    rzp.on("payment.failed", function (resp) {
      console.error("Payment failed event:", resp);
      showToast(resp.error?.description || "Payment was not completed.", "error");
      stopOrderPolling();
    });
    rzp.open();
  } catch (e) {
    console.error("Razorpay open error:", e);
    showToast("Failed to open Razorpay modal: " + e.message, "error");
    stopOrderPolling();
  }
}

function startOrderPolling(orderId) {
  stopOrderPolling();
  let attempts = 0;
  const maxAttempts = 45; // 45 * 4s = 3 minutes

  orderPollingTimer = setInterval(async () => {
    attempts++;
    if (attempts > maxAttempts || !state.token) {
      stopOrderPolling();
      return;
    }

    try {
      const res = await fetch("/subscription/check-order", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${state.token}`,
        },
        body: JSON.stringify({ order_id: orderId }),
      });

      if (res.ok) {
        const data = await res.json();
        if (data.is_active) {
          stopOrderPolling();
          showToast("🎉 Payment detected! Monthly Pro is now ACTIVE!", "success");
          await loadSubscriptionStatus();
          navigateTo("tutor");
        }
      }
    } catch (err) {
      // Ignore background poll errors
    }
  }, 4000);
}

function stopOrderPolling() {
  if (orderPollingTimer) {
    clearInterval(orderPollingTimer);
    orderPollingTimer = null;
  }
}

async function checkPaymentStatusManual(orderId = null, showLoaderModal = true) {
  if (!state.token) {
    showToast("Please sign in first with the email used during payment.", "info");
    openAccountModal();
    return;
  }

  const targetOrderId = orderId || localStorage.getItem("vb_last_order_id") || null;

  if (showLoaderModal) {
    showLoader("Checking payment status directly with Razorpay gateway...");
  }

  try {
    const res = await fetch("/subscription/check-order", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${state.token}`,
      },
      body: JSON.stringify({ order_id: targetOrderId }),
    });

    if (showLoaderModal) hideLoader();

    if (res.ok) {
      const data = await res.json();
      if (data.is_active) {
        showToast("🎉 Verified! Your Monthly Pro Plan is ACTIVE!", "success");
        await loadSubscriptionStatus();
        navigateTo("tutor");
      } else {
        showToast("Payment status: Not yet completed.", "info");
      }
    } else {
      const err = await res.json().catch(() => ({ detail: "Order status check failed" }));
      if (showLoaderModal) {
        showToast(err.detail || "Payment not yet confirmed. If you just paid, please wait a moment.", "error");
      }
    }
  } catch (err) {
    if (showLoaderModal) hideLoader();
    if (showLoaderModal) showToast("Error connecting to server to check payment status.", "error");
  }
}

async function confirmPaymentOnServer(payload) {
  showLoader("Verifying payment signature with Razorpay & activating subscription...");

  try {
    const res = await fetch("/subscription/confirm", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${state.token}`,
      },
      body: JSON.stringify(payload),
    });

    hideLoader();

    if (res.ok) {
      showToast("🎉 Congratulations! Your Monthly Pro Plan is now ACTIVE!", "success");
      await loadSubscriptionStatus();
      navigateTo("tutor");
    } else {
      const err = await res.json().catch(() => ({ detail: "Verification failed" }));
      showToast(err.detail || "Payment verification failed.", "error");
    }
  } catch (err) {
    hideLoader();
    showToast("Error confirming payment with server.", "error");
  }
}


// ── Instant Simulated Payment (Dev & Testing) ──────────────────────────────
async function startSimulatedPayment() {
  if (!state.token) {
    showToast("Please sign in first to attach subscription.", "info");
    openAccountModal();
    return;
  }

  showLoader("Simulating test payment and activating Pro...");

  try {
    // 1. Create order
    const res = await fetch("/subscription/initiate", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${state.token}`,
      },
      body: JSON.stringify({ plan: "monthly" }),
    });

    if (!res.ok) {
      hideLoader();
      showToast("Failed to initiate simulated order.", "error");
      return;
    }

    const orderOut = await res.json();

    // 2. Direct verify / confirm (simulate callback)
    const mockPaymentId = "pay_mock_" + Math.random().toString(36).substring(2, 12);
    const confirmRes = await fetch("/subscription/confirm", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${state.token}`,
      },
      body: JSON.stringify({
        order_id: orderOut.order_id,
        payment_id: mockPaymentId,
        signature: "mock_signature_dev_pass",
      }),
    });

    hideLoader();

    if (confirmRes.ok) {
      showToast("⚡ Test Pro Plan activated successfully for 30 days!", "success");
      await loadSubscriptionStatus();
      navigateTo("tutor");
    } else {
      const err = await confirmRes.json().catch(() => ({ detail: "Verification failed" }));
      showToast(err.detail || "Simulated payment verification failed.", "error");
    }
  } catch (err) {
    hideLoader();
    showToast("Network error during simulated payment.", "error");
  }
}

// ── UI Helpers & Utilities ──────────────────────────────────────────────────
function showLoader(text) {
  const loader = document.getElementById("tutorLoader");
  const loaderText = document.getElementById("loaderText");
  if (loader && loaderText) {
    loaderText.textContent = text || "Processing...";
    loader.classList.remove("hidden");
  }
}

function hideLoader() {
  const loader = document.getElementById("tutorLoader");
  if (loader) {
    loader.classList.add("hidden");
  }
}

function showToast(message, type = "info") {
  const container = document.getElementById("toastContainer");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  
  let icon = "fa-info-circle";
  if (type === "success") icon = "fa-circle-check";
  if (type === "error") icon = "fa-circle-exclamation";

  toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateX(100%)";
    toast.style.transition = "all 0.3s ease";
    setTimeout(() => toast.remove(), 300);
  }, 4500);
}

function copyText(elementId) {
  const text = document.getElementById(elementId)?.textContent || "";
  if (text) {
    navigator.clipboard.writeText(text).then(() => {
      showToast("Copied to clipboard!", "success");
    });
  }
}

// =============================================================================
// FEEDBACK FORM
// =============================================================================

let feedbackRating = 0;

function setRating(value) {
  feedbackRating = value;
  const stars = document.querySelectorAll(".star");
  const hints = ["", "Poor", "Fair", "Good", "Very Good", "Excellent!"];
  stars.forEach((star, index) => {
    star.classList.toggle("active", index < value);
  });
  const hint = document.getElementById("starHint");
  if (hint) hint.textContent = hints[value] || "Click a star to rate";
}

async function submitFeedback() {
  const name     = document.getElementById("fbName")?.value.trim();
  const email    = document.getElementById("fbEmail")?.value.trim();
  const category = document.getElementById("fbCategory")?.value;
  const message  = document.getElementById("fbMessage")?.value.trim();

  if (!name || !email || !message) {
    showToast("Please fill in all required fields.", "error"); return;
  }
  if (feedbackRating === 0) {
    showToast("Please select a star rating.", "error"); return;
  }

  const btn = document.getElementById("btnSubmitFeedback");
  const btnText = document.getElementById("feedbackBtnText");
  const spinner = document.getElementById("feedbackSpinner");
  btn.disabled = true;
  btnText.textContent = "Submitting…";
  spinner.classList.remove("hidden");

  try {
    const res = await fetch("/feedback/submit", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name, email, rating: feedbackRating, category, message,
        page_source: "web"
      })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.detail || "Submission failed");

    // Show success state
    document.getElementById("feedbackFormBody").classList.add("hidden");
    const successBox = document.getElementById("feedbackSuccess");
    successBox.classList.remove("hidden");
    document.getElementById("feedbackSuccessMsg").textContent = data.message;
    document.getElementById("feedbackTicketId").textContent = `Ticket: ${data.ticket_id}`;
    showToast("Feedback submitted successfully!", "success");

  } catch (err) {
    showToast(`Error: ${err.message}`, "error");
    btn.disabled = false;
    btnText.textContent = "Submit Feedback";
    spinner.classList.add("hidden");
  }
}

function resetFeedbackForm() {
  document.getElementById("feedbackFormBody").classList.remove("hidden");
  document.getElementById("feedbackSuccess").classList.add("hidden");
  document.getElementById("fbName").value = "";
  document.getElementById("fbEmail").value = "";
  document.getElementById("fbMessage").value = "";
  document.getElementById("fbCategory").value = "general";
  feedbackRating = 0;
  setRating(0);
  const btn = document.getElementById("btnSubmitFeedback");
  const btnText = document.getElementById("feedbackBtnText");
  const spinner = document.getElementById("feedbackSpinner");
  btn.disabled = false;
  btnText.textContent = "Submit Feedback";
  spinner.classList.add("hidden");
}

// =============================================================================
// HELP FORM
// =============================================================================

async function submitHelp() {
  const name        = document.getElementById("helpName")?.value.trim();
  const email       = document.getElementById("helpEmail")?.value.trim();
  const issueType   = document.getElementById("helpIssueType")?.value;
  const subject     = document.getElementById("helpSubject")?.value.trim();
  const description = document.getElementById("helpDescription")?.value.trim();
  const device      = document.getElementById("helpDevice")?.value;

  if (!name || !email || !subject || !description) {
    showToast("Please fill in all required fields.", "error"); return;
  }

  const btn = document.getElementById("btnSubmitHelp");
  const btnText = document.getElementById("helpBtnText");
  const spinner = document.getElementById("helpSpinner");
  btn.disabled = true;
  btnText.textContent = "Sending…";
  spinner.classList.remove("hidden");

  try {
    const res = await fetch("/feedback/help", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name, email,
        issue_type: issueType,
        subject, description, device,
        page_source: "web"
      })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.detail || "Submission failed");

    // Show success state
    document.getElementById("helpFormBody").classList.add("hidden");
    const successBox = document.getElementById("helpSuccess");
    successBox.classList.remove("hidden");
    document.getElementById("helpSuccessMsg").textContent = data.message;
    document.getElementById("helpTicketId").textContent = `Ticket: ${data.ticket_id}`;
    showToast("Help request sent!", "success");

  } catch (err) {
    showToast(`Error: ${err.message}`, "error");
    btn.disabled = false;
    btnText.textContent = "Send Help Request";
    spinner.classList.add("hidden");
  }
}

function resetHelpForm() {
  document.getElementById("helpFormBody").classList.remove("hidden");
  document.getElementById("helpSuccess").classList.add("hidden");
  ["helpName","helpEmail","helpSubject","helpDescription"].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.value = "";
  });
  const btn = document.getElementById("btnSubmitHelp");
  const btnText = document.getElementById("helpBtnText");
  const spinner = document.getElementById("helpSpinner");
  btn.disabled = false;
  btnText.textContent = "Send Help Request";
  spinner.classList.add("hidden");
}

// =============================================================================
// FAQ ACCORDION
// =============================================================================

function toggleFaq(card) {
  const isOpen = card.classList.contains("open");
  // Close all
  document.querySelectorAll(".faq-card.open").forEach(c => c.classList.remove("open"));
  // Open clicked (unless it was already open)
  if (!isOpen) card.classList.add("open");
}

