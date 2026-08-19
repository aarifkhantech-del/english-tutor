"""
Feedback & Help Submission API
Collects user feedback and help requests, stores to DB, and emails vocalbharat91@gmail.com
"""

import logging
import smtplib
from datetime import datetime, timezone
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from typing import Optional

from fastapi import APIRouter, HTTPException, Request, status
from pydantic import BaseModel, EmailStr, validator

from app.core.config import settings

logger = logging.getLogger("english_tutor.feedback")

router = APIRouter(prefix="/feedback", tags=["Feedback & Help"])

OWNER_EMAIL = "vocalbharat91@gmail.com"
APP_NAME = "VocalBharat"


# ── Pydantic Models ──────────────────────────────────────────────────────────

class FeedbackRequest(BaseModel):
    name: str
    email: EmailStr
    rating: int  # 1–5 stars
    category: str  # "general" | "feature" | "bug" | "praise"
    message: str
    page_source: Optional[str] = "web"  # "web" | "android"

    @validator("rating")
    def validate_rating(cls, v):
        if not 1 <= v <= 5:
            raise ValueError("Rating must be between 1 and 5")
        return v

    @validator("name")
    def validate_name(cls, v):
        if not v or not v.strip():
            raise ValueError("Name is required")
        return v.strip()[:100]

    @validator("message")
    def validate_message(cls, v):
        if not v or not v.strip():
            raise ValueError("Message is required")
        return v.strip()[:2000]


class HelpRequest(BaseModel):
    name: str
    email: EmailStr
    issue_type: str  # "payment" | "account" | "technical" | "audio" | "other"
    subject: str
    description: str
    device: Optional[str] = None  # "android" | "web" | "ios"
    app_version: Optional[str] = None
    page_source: Optional[str] = "web"

    @validator("name")
    def validate_name(cls, v):
        return v.strip()[:100]

    @validator("subject")
    def validate_subject(cls, v):
        if not v or not v.strip():
            raise ValueError("Subject is required")
        return v.strip()[:200]

    @validator("description")
    def validate_description(cls, v):
        if not v or not v.strip():
            raise ValueError("Description is required")
        return v.strip()[:3000]


class FeedbackResponse(BaseModel):
    success: bool
    message: str
    ticket_id: str


# ── Email Sending ────────────────────────────────────────────────────────────

def _generate_ticket_id(prefix: str) -> str:
    now = datetime.now(timezone.utc)
    return f"{prefix}-{now.strftime('%Y%m%d%H%M%S')}"


def _send_email(to: str, subject: str, html_body: str, text_body: str) -> bool:
    """Send email via configured SMTP or fall back to console logging."""
    if settings.use_console_email:
        # Dev mode: print to console
        logger.info("=" * 60)
        logger.info("📧 EMAIL (console fallback — configure SMTP to send real emails)")
        logger.info("To: %s", to)
        logger.info("Subject: %s", subject)
        logger.info("Body:\n%s", text_body)
        logger.info("=" * 60)
        return True

    try:
        msg = MIMEMultipart("alternative")
        msg["From"] = settings.EMAIL_FROM
        msg["To"] = to
        msg["Subject"] = subject
        msg["Reply-To"] = to

        msg.attach(MIMEText(text_body, "plain", "utf-8"))
        msg.attach(MIMEText(html_body, "html", "utf-8"))

        with smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT, timeout=10) as smtp:
            smtp.ehlo()
            smtp.starttls()
            smtp.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
            smtp.sendmail(settings.SMTP_USER, to, msg.as_string())

        logger.info("Email sent to %s | subject=%s", to, subject)
        return True

    except smtplib.SMTPAuthenticationError:
        logger.error("SMTP authentication failed — check SMTP_USER/SMTP_PASSWORD in .env")
        return False
    except Exception as e:
        logger.error("Failed to send email to %s: %s", to, e)
        return False


# ── Feedback Email Template ──────────────────────────────────────────────────

STAR_MAP = {1: "⭐", 2: "⭐⭐", 3: "⭐⭐⭐", 4: "⭐⭐⭐⭐", 5: "⭐⭐⭐⭐⭐"}

def _feedback_html(fb: FeedbackRequest, ticket_id: str) -> str:
    stars = STAR_MAP.get(fb.rating, "")
    return f"""
<!DOCTYPE html>
<html>
<body style="font-family: Arial, sans-serif; background:#0A0E1A; color:#F8FAFC; margin:0; padding:0;">
  <div style="max-width:600px; margin:0 auto; padding:30px;">
    <div style="background:linear-gradient(135deg,#2979FF,#00F5D4); padding:24px; border-radius:16px 16px 0 0; text-align:center;">
      <h1 style="margin:0; font-size:24px; color:#fff;">🎙️ New User Feedback</h1>
      <p style="margin:6px 0 0; color:rgba(255,255,255,0.8); font-size:13px;">{APP_NAME} — AI Spoken English Tutor</p>
    </div>
    <div style="background:#0D1B2E; padding:28px; border-radius:0 0 16px 16px; border:1px solid rgba(255,255,255,0.08); border-top:none;">
      <p style="color:#94A3B8; font-size:13px; margin:0 0 20px;">Ticket: <strong style="color:#fff;">{ticket_id}</strong> &nbsp;|&nbsp; Received: {datetime.now(timezone.utc).strftime("%d %b %Y, %H:%M UTC")}</p>

      <table style="width:100%; border-collapse:collapse;">
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px; width:120px;">👤 Name</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;"><strong>{fb.name}</strong></td>
        </tr>
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px;">📧 Email</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;"><a href="mailto:{fb.email}" style="color:#60A5FA;">{fb.email}</a></td>
        </tr>
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px;">⭐ Rating</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:18px;">{stars} ({fb.rating}/5)</td>
        </tr>
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px;">🏷️ Category</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;">{fb.category.title()}</td>
        </tr>
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px;">📱 Source</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;">{fb.page_source.title()}</td>
        </tr>
      </table>

      <div style="margin-top:20px; padding:16px; background:rgba(255,255,255,0.03); border:1px solid rgba(255,255,255,0.06); border-radius:10px;">
        <p style="margin:0 0 8px; color:#94A3B8; font-size:12px; text-transform:uppercase; letter-spacing:0.05em;">💬 Feedback Message</p>
        <p style="margin:0; color:#fff; font-size:15px; line-height:1.6; white-space:pre-wrap;">{fb.message}</p>
      </div>

      <div style="margin-top:24px; padding:14px; background:rgba(41,121,255,0.08); border:1px solid rgba(41,121,255,0.3); border-radius:10px; text-align:center;">
        <p style="margin:0; color:#60A5FA; font-size:13px;">Reply directly to this email to respond to the user.</p>
      </div>
    </div>
  </div>
</body>
</html>
"""

def _feedback_text(fb: FeedbackRequest, ticket_id: str) -> str:
    return f"""
VocalBharat — New User Feedback
================================
Ticket: {ticket_id}
Time: {datetime.now(timezone.utc).strftime("%d %b %Y, %H:%M UTC")}

Name: {fb.name}
Email: {fb.email}
Rating: {fb.rating}/5 stars
Category: {fb.category}
Source: {fb.page_source}

Message:
{fb.message}

---
Reply to this email to contact the user directly.
"""


# ── Help Email Template ──────────────────────────────────────────────────────

def _help_html(h: HelpRequest, ticket_id: str) -> str:
    return f"""
<!DOCTYPE html>
<html>
<body style="font-family: Arial, sans-serif; background:#0A0E1A; color:#F8FAFC; margin:0; padding:0;">
  <div style="max-width:600px; margin:0 auto; padding:30px;">
    <div style="background:linear-gradient(135deg,#FF5252,#FF8F00); padding:24px; border-radius:16px 16px 0 0; text-align:center;">
      <h1 style="margin:0; font-size:24px; color:#fff;">🆘 Help Request Received</h1>
      <p style="margin:6px 0 0; color:rgba(255,255,255,0.85); font-size:13px;">{APP_NAME} — User Support</p>
    </div>
    <div style="background:#0D1B2E; padding:28px; border-radius:0 0 16px 16px; border:1px solid rgba(255,255,255,0.08); border-top:none;">
      <p style="color:#94A3B8; font-size:13px; margin:0 0 20px;">Ticket: <strong style="color:#fff;">{ticket_id}</strong> &nbsp;|&nbsp; Received: {datetime.now(timezone.utc).strftime("%d %b %Y, %H:%M UTC")}</p>

      <table style="width:100%; border-collapse:collapse;">
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px; width:130px;">👤 Name</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;"><strong>{h.name}</strong></td>
        </tr>
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px;">📧 Email</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;"><a href="mailto:{h.email}" style="color:#60A5FA;">{h.email}</a></td>
        </tr>
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px;">🔧 Issue Type</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;">{h.issue_type.title()}</td>
        </tr>
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px;">📋 Subject</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;"><strong>{h.subject}</strong></td>
        </tr>
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px;">📱 Device</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;">{h.device or "Not specified"}</td>
        </tr>
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px;">🔢 App Version</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;">{h.app_version or "Not specified"}</td>
        </tr>
        <tr>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#94A3B8; font-size:13px;">🌐 Source</td>
          <td style="padding:10px 0; border-bottom:1px solid rgba(255,255,255,0.06); color:#fff; font-size:14px;">{h.page_source.title()}</td>
        </tr>
      </table>

      <div style="margin-top:20px; padding:16px; background:rgba(255,255,255,0.03); border:1px solid rgba(255,255,255,0.06); border-radius:10px;">
        <p style="margin:0 0 8px; color:#94A3B8; font-size:12px; text-transform:uppercase; letter-spacing:0.05em;">📝 Problem Description</p>
        <p style="margin:0; color:#fff; font-size:15px; line-height:1.6; white-space:pre-wrap;">{h.description}</p>
      </div>

      <div style="margin-top:24px; padding:14px; background:rgba(255,82,82,0.08); border:1px solid rgba(255,82,82,0.3); border-radius:10px; text-align:center;">
        <p style="margin:0; color:#FF8A80; font-size:13px;">⚡ Reply directly to this email to assist the user. Use ticket ID <strong>{ticket_id}</strong> in subject.</p>
      </div>
    </div>
  </div>
</body>
</html>
"""

def _help_text(h: HelpRequest, ticket_id: str) -> str:
    return f"""
VocalBharat — Help Request
============================
Ticket: {ticket_id}
Time: {datetime.now(timezone.utc).strftime("%d %b %Y, %H:%M UTC")}

Name: {h.name}
Email: {h.email}
Issue Type: {h.issue_type}
Subject: {h.subject}
Device: {h.device or "Not specified"}
App Version: {h.app_version or "Not specified"}
Source: {h.page_source}

Description:
{h.description}

---
Reply to this email to assist the user. Ticket ID: {ticket_id}
"""


# ── API Endpoints ────────────────────────────────────────────────────────────

@router.post(
    "/submit",
    response_model=FeedbackResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Submit user feedback",
    description="Accepts user feedback with rating and message, and emails the owner.",
)
async def submit_feedback(payload: FeedbackRequest, request: Request):
    """Submit user feedback and notify the app owner via email."""
    ticket_id = _generate_ticket_id("FB")

    logger.info(
        "Feedback received | ticket=%s | from=%s | rating=%d | category=%s | source=%s",
        ticket_id, payload.email, payload.rating, payload.category, payload.page_source
    )

    html = _feedback_html(payload, ticket_id)
    text = _feedback_text(payload, ticket_id)
    subject = f"[{APP_NAME} Feedback] {payload.rating}⭐ — {payload.category.title()} from {payload.name} | {ticket_id}"

    email_sent = _send_email(OWNER_EMAIL, subject, html, text)

    if not email_sent:
        logger.warning("Failed to send feedback email for ticket %s", ticket_id)

    return FeedbackResponse(
        success=True,
        message="Thank you for your feedback! We appreciate your input and will review it shortly.",
        ticket_id=ticket_id,
    )


@router.post(
    "/help",
    response_model=FeedbackResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Submit a help request",
    description="Collects user issue details and emails the owner to follow up.",
)
async def submit_help_request(payload: HelpRequest, request: Request):
    """Submit a help/support request and notify the app owner via email."""
    ticket_id = _generate_ticket_id("HELP")

    logger.info(
        "Help request received | ticket=%s | from=%s | issue=%s | device=%s",
        ticket_id, payload.email, payload.issue_type, payload.device
    )

    html = _help_html(payload, ticket_id)
    text = _help_text(payload, ticket_id)
    subject = f"[{APP_NAME} Help] {payload.issue_type.title()} — {payload.subject} | {ticket_id}"

    email_sent = _send_email(OWNER_EMAIL, subject, html, text)

    if not email_sent:
        logger.warning("Failed to send help email for ticket %s", ticket_id)

    return FeedbackResponse(
        success=True,
        message=(
            "Your help request has been received. Our support team will contact you "
            f"at {payload.email} within 24 hours. Your ticket ID is: {ticket_id}"
        ),
        ticket_id=ticket_id,
    )
