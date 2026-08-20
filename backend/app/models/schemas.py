from pydantic import BaseModel, Field, EmailStr
from typing import Optional
from datetime import datetime


class CorrectionResult(BaseModel):
    hindi_input: str = Field(..., description="Original Hindi/Hinglish speech transcribed")
    english_translation: str = Field(..., description="Natural English translation")
    corrected: str = Field(..., description="Corrected English sentence")
    explanation: str = Field(default="", description="Short plain text grammar/vocabulary tip")
    practice: str = Field(default="", description="English practice sentence")
    encouragement: str = Field(default="", description="Motivational feedback note")


class TutorResponse(BaseModel):
    transcription: str = Field(..., description="Transcribed student Hindi text")
    correction: CorrectionResult = Field(..., description="AI tutor correction and explanation")
    audio_b64: str = Field(default="", description="Base64-encoded in-memory MP3 audio pronunciation")
    requests_used: int = 0
    requests_limit: int = 8
    requests_remaining: int = 8
    quota_exceeded: bool = False


class TextTutorRequest(BaseModel):
    text: str = Field(..., description="Hindi/Hinglish text to translate and tutor")


class TranscribeResponse(BaseModel):
    text: str = Field(..., description="Transcribed text from speech")


class HealthResponse(BaseModel):
    status: str = "healthy"
    whisper: str = "loaded"
    mistral: str = "configured"
    tts: str = "configured"
    version: str = "2.1.0"


# -- Grammar Explainer Schemas -------------------------------------------------

class GrammarRequest(BaseModel):
    topic: str = Field(..., description="English grammar topic, e.g. 'past tense'")


class GrammarExample(BaseModel):
    sentence: str = Field(..., description="English example sentence")
    explanation: str = Field(..., description="Why this sentence demonstrates the rule")


class GrammarResponse(BaseModel):
    topic: str = Field(..., description="Normalized grammar topic name")
    definition: str = Field(..., description="Clear English definition of the grammar rule")
    hindi_definition: str = Field(..., description="Hindi translation of the definition")
    examples: list[GrammarExample] = Field(default_factory=list, description="3 examples")
    tips: list[str] = Field(default_factory=list, description="2-3 practical tips")
    difficulty: str = Field(default="Beginner", description="Beginner / Intermediate / Advanced")
    requests_used: int = 0
    requests_limit: int = 8
    requests_remaining: int = 8
    quota_exceeded: bool = False


# -- Auth / OTP Schemas --------------------------------------------------------

class OTPRequestIn(BaseModel):
    """Request body for sending an OTP to an email address."""
    email: EmailStr = Field(..., description="User's email address")


class OTPRequestOut(BaseModel):
    """Response after requesting an OTP."""
    message: str
    email: str


class OTPVerifyIn(BaseModel):
    """Request body for verifying the OTP and receiving a JWT."""
    email: EmailStr = Field(..., description="Same email used in the OTP request")
    otp: str = Field(..., min_length=6, max_length=6, description="6-digit OTP")


class GoogleSignInIn(BaseModel):
    """Google OpenID Connect ID token returned by the Google client SDK."""
    id_token: str = Field(..., min_length=20)


class GoogleSignInConfigOut(BaseModel):
    enabled: bool
    client_id: Optional[str] = None


class TokenOut(BaseModel):
    """JWT access token returned after successful OTP verification."""
    access_token: str
    token_type: str = "bearer"
    email: str
    is_new_user: bool = Field(description="True if the account was just created")


class UserOut(BaseModel):
    """Current authenticated user info."""
    id: str
    email: str
    first_name: str = ""
    last_name: str = ""
    full_name: str = ""
    avatar_url: str = ""
    is_active: bool
    created_at: datetime
    last_login_at: Optional[datetime] = None

    class Config:
        from_attributes = True


# -- Subscription / Payment Schemas --------------------------------------------

class PlanInfo(BaseModel):
    """Description of a single subscription plan."""
    id: str                  # "monthly", "yearly", etc.
    name: str
    description: str
    amount: int              # in INR
    currency: str
    duration_days: int
    badge: Optional[str] = None  # e.g. "Most Popular"


class PlansOut(BaseModel):
    plans: list[PlanInfo]


class InitiatePaymentIn(BaseModel):
    """Request body to initiate a payment for a chosen plan."""
    plan: str = Field(default="monthly", description="Subscription plan ID (e.g. 'monthly')")


class InitiatePaymentOut(BaseModel):
    """Order details returned to the frontend to open the payment checkout."""
    order_id: str                        # gateway order ID (or mock ID)
    subscription_id: str                 # internal subscription ID
    gateway: str                         # "mock", "razorpay", etc.
    amount: int                          # in INR
    currency: str
    gateway_key: Optional[str] = None    # public key for frontend SDK (e.g. Razorpay key_id)


class ConfirmPaymentIn(BaseModel):
    """Payment confirmation details sent by the frontend after user completes payment."""
    order_id: str
    payment_id: str
    signature: Optional[str] = None      # HMAC signature (required for Razorpay)


class CheckOrderIn(BaseModel):
    """Request to verify the status of an order directly with the payment gateway."""
    order_id: Optional[str] = Field(default=None, description="Gateway order ID. If omitted, latest pending order is checked.")



class SubscriptionStatusOut(BaseModel):
    """Current subscription state and usage quota for the user."""
    has_subscription: bool
    plan: Optional[str] = None           # "monthly", etc.
    status: Optional[str] = None         # "active", "expired", "cancelled"
    is_active: bool
    starts_at: Optional[datetime] = None
    expires_at: Optional[datetime] = None
    days_remaining: int = 0
    requests_used: int = 0               # Number of requests used
    requests_limit: int = 8              # Free tier limit (8 requests)
    requests_remaining: int = 8          # Remaining free requests
    quota_exceeded: bool = False         # True if >= 8 and no active subscription


class WebhookPayload(BaseModel):
    """Generic webhook payload — each gateway sends its own shape; stored as-is."""
    event: Optional[str] = None
    order_id: Optional[str] = None
    payment_id: Optional[str] = None
    status: Optional[str] = None
