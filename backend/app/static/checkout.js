/**
 * Razorpay Standard Web Checkout Integration
 * VocalBharat - Spoken English Tutor
 */

// Application state
const state = {
  keyId: '',
  currency: 'INR',
  selectedAmountRupees: 120,
  selectedAmountPaise: 12000,
  selectedPlanName: 'Monthly Pro',
  isProcessing: false,
};

// DOM Elements
const elements = {
  tierMonthly: document.getElementById('tierMonthly'),
  tierTest: document.getElementById('tierTest'),
  displayAmount: document.getElementById('displayAmount'),
  summaryPlanName: document.getElementById('summaryPlanName'),
  summaryAmount: document.getElementById('summaryAmount'),
  summaryPaise: document.getElementById('summaryPaise'),
  summaryTotal: document.getElementById('summaryTotal'),
  payButton: document.getElementById('payButton'),
  payButtonText: document.getElementById('payButtonText'),
  paySpinner: document.getElementById('paySpinner'),
  statusAlert: document.getElementById('statusAlert'),
  statusIcon: document.getElementById('statusIcon'),
  statusTitle: document.getElementById('statusTitle'),
  statusMessage: document.getElementById('statusMessage'),
  paymentDetails: document.getElementById('paymentDetails'),
  resOrderId: document.getElementById('resOrderId'),
  resPaymentId: document.getElementById('resPaymentId'),
  resSignature: document.getElementById('resSignature'),
  userName: document.getElementById('userName'),
  userEmail: document.getElementById('userEmail'),
  userPhone: document.getElementById('userPhone'),
};

/**
 * Initialize on page load: fetch backend config & bind event listeners
 */
document.addEventListener('DOMContentLoaded', async () => {
  await fetchPaymentConfig();
  setupPlanSelectors();
});

/**
 * Fetch public payment configuration (key_id, default currency)
 */
async function fetchPaymentConfig() {
  try {
    const res = await fetch('/api/payment-config');
    if (res.ok) {
      const data = await res.json();
      state.keyId = data.key_id;
      state.currency = data.currency || 'INR';
    }
  } catch (err) {
    console.warn('Could not pre-fetch payment config:', err);
  }
}

/**
 * Configure plan toggle selector
 */
function setupPlanSelectors() {
  elements.tierMonthly.addEventListener('click', () => selectPlan(120, 'Monthly Pro', elements.tierMonthly, elements.tierTest));
  elements.tierTest.addEventListener('click', () => selectPlan(1, 'Test ₹1.00', elements.tierTest, elements.tierMonthly));
}

function selectPlan(rupees, planName, activeEl, inactiveEl) {
  state.selectedAmountRupees = rupees;
  state.selectedAmountPaise = rupees * 100;
  state.selectedPlanName = planName;

  activeEl.classList.add('active');
  inactiveEl.classList.remove('active');

  elements.displayAmount.textContent = rupees;
  elements.summaryPlanName.textContent = planName;
  elements.summaryAmount.textContent = `₹${rupees.toFixed(2)}`;
  elements.summaryPaise.textContent = `${state.selectedAmountPaise.toLocaleString()} paise`;
  elements.summaryTotal.textContent = `₹${rupees.toFixed(2)}`;
  elements.payButtonText.textContent = `Pay ₹${rupees} with Razorpay`;

  hideStatusAlert();
}

/**
 * Initiate Razorpay Standard Checkout Flow
 */
async function initiateCheckout() {
  if (state.isProcessing) return;

  const name = elements.userName.value.trim();
  const email = elements.userEmail.value.trim();
  const phone = elements.userPhone.value.trim();

  if (!email || !phone) {
    showStatusAlert('warning', 'Missing Information', 'Please provide a valid email and phone number.');
    return;
  }

  setLoadingState(true);
  hideStatusAlert();

  try {
    // ── STEP 1: BACKEND - Create Order ───────────────────────────────────────
    const createOrderResponse = await fetch('/api/create-order', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        amount: state.selectedAmountPaise,
        currency: state.currency,
        receipt: `rcpt_${Date.now()}`,
        notes: {
          customer_name: name,
          plan: state.selectedPlanName,
        },
      }),
    });

    const orderData = await createOrderResponse.json();

    if (!createOrderResponse.ok) {
      throw new Error(orderData.detail || orderData.message || 'Failed to create order on server.');
    }

    const { order_id, amount, currency, key_id } = orderData;
    const activeKeyId = key_id || state.keyId;

    if (!activeKeyId) {
      throw new Error('Razorpay Key ID is not configured on the backend.');
    }

    if (typeof Razorpay === 'undefined') {
      throw new Error('Razorpay SDK failed to load. Please check your internet connection.');
    }

    // ── STEP 2: FRONTEND - Open Checkout Modal ──────────────────────────────
    const options = {
      key: activeKeyId,
      amount: amount,
      currency: currency,
      name: 'VocalBharat',
      description: `Spoken English Subscription (${state.selectedPlanName})`,
      image: 'https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/1f399.png',
      order_id: order_id,
      prefill: {
        name: name,
        email: email,
        contact: phone,
      },
      notes: {
        plan: state.selectedPlanName,
        source: 'standard_web_checkout',
      },
      theme: {
        color: '#4f46e5',
        backdrop_color: 'rgba(11, 15, 25, 0.85)',
      },
      modal: {
        ondismiss: function () {
          handleModalDismiss();
        },
      },
      handler: async function (response) {
        // Step 2 success callback -> passes razorpay_payment_id, razorpay_order_id, razorpay_signature
        await handlePaymentSuccess(response);
      },
    };

    const rzp = new Razorpay(options);

    // Step 2: Handle payment failure event
    rzp.on('payment.failed', function (response) {
      handlePaymentFailure(response);
    });

    rzp.open();
  } catch (error) {
    console.error('Checkout error:', error);
    setLoadingState(false);
    showStatusAlert('error', 'Checkout Error', error.message || 'An unexpected error occurred.');
  }
}

/**
 * ── STEP 3: BACKEND - Verify Signature ───────────────────────────────────────
 */
async function handlePaymentSuccess(response) {
  setLoadingState(true);
  showStatusAlert('warning', 'Verifying Payment...', 'Validating Razorpay HMAC signature with backend.');

  try {
    const verifyRes = await fetch('/api/verify-payment', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        razorpay_order_id: response.razorpay_order_id,
        razorpay_payment_id: response.razorpay_payment_id,
        razorpay_signature: response.razorpay_signature,
      }),
    });

    const verifyData = await verifyRes.json();

    if (!verifyRes.ok) {
      throw new Error(verifyData.detail || verifyData.message || 'Signature verification failed.');
    }

    // Success state
    setLoadingState(false);
    showStatusAlert(
      'success',
      '🎉 Payment Successful & Verified!',
      'Your payment signature was verified by the backend. Subscription is active.'
    );

    // Populate transaction details
    elements.resOrderId.textContent = response.razorpay_order_id;
    elements.resPaymentId.textContent = response.razorpay_payment_id;
    elements.resSignature.textContent = response.razorpay_signature ? `${response.razorpay_signature.slice(0, 20)}...` : 'N/A';
    elements.paymentDetails.classList.remove('hidden');
  } catch (error) {
    console.error('Verification error:', error);
    setLoadingState(false);
    showStatusAlert('error', 'Verification Failed', error.message || 'Could not verify payment signature.');
  }
}

/**
 * Handle Modal Dismissal (User cancelled or closed the modal)
 */
function handleModalDismiss() {
  setLoadingState(false);
  showStatusAlert(
    'warning',
    'Payment Cancelled',
    'The Razorpay checkout modal was closed before completing the payment.'
  );
}

/**
 * Handle Payment Failure Event
 */
function handlePaymentFailure(response) {
  setLoadingState(false);
  const reason = response.error ? response.error.description : 'Payment could not be processed.';
  const code = response.error ? response.error.code : 'UNKNOWN_ERROR';
  showStatusAlert('error', `Payment Failed (${code})`, reason);
}

/**
 * UI State Helpers
 */
function setLoadingState(isLoading) {
  state.isProcessing = isLoading;
  elements.payButton.disabled = isLoading;
  if (isLoading) {
    elements.paySpinner.classList.add('active');
  } else {
    elements.paySpinner.classList.remove('active');
  }
}

function showStatusAlert(type, title, message) {
  elements.statusAlert.className = `status-alert ${type}`;
  elements.statusTitle.textContent = title;
  elements.statusMessage.textContent = message;

  if (type === 'success') {
    elements.statusIcon.textContent = '✅';
  } else if (type === 'error') {
    elements.statusIcon.textContent = '❌';
    elements.paymentDetails.classList.add('hidden');
  } else if (type === 'warning') {
    elements.statusIcon.textContent = '⚠️';
    elements.paymentDetails.classList.add('hidden');
  }

  elements.statusAlert.classList.remove('hidden');
}

function hideStatusAlert() {
  elements.statusAlert.classList.add('hidden');
  elements.paymentDetails.classList.add('hidden');
}
