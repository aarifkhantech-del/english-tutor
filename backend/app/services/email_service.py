"""Email sending service — console mode (dev) and SMTP mode (production)."""
import logging
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText

from app.core.config import settings

logger = logging.getLogger("english_tutor.email")


def _build_otp_html(otp: str, email: str) -> str:
    return f"""
<!DOCTYPE html>
<html>
<head><meta charset="utf-8"></head>
<body style="margin:0;padding:0;background:#0f0c29;font-family:Inter,Arial,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0">
    <tr><td align="center" style="padding:40px 20px;">
      <table width="520" style="background:linear-gradient(135deg,#1a1a2e,#16213e);border-radius:16px;
             border:1px solid rgba(139,92,246,0.3);overflow:hidden;">
        <tr>
          <td style="background:linear-gradient(90deg,#7c3aed,#4f46e5);padding:28px 40px;text-align:center;">
            <h1 style="margin:0;color:#fff;font-size:24px;letter-spacing:1px;">🎓 VocalBharat</h1>
            <p style="margin:6px 0 0;color:rgba(255,255,255,0.8);font-size:14px;">Your English learning journey awaits</p>
          </td>
        </tr>
        <tr>
          <td style="padding:40px;">
            <p style="color:#e2e8f0;font-size:16px;margin:0 0 24px;">Hi there!</p>
            <p style="color:#94a3b8;font-size:15px;margin:0 0 32px;">
              Use this one-time password to log in to your VocalBharat account. 
              This code expires in <strong style="color:#a78bfa;">10 minutes</strong>.
            </p>
            <div style="background:rgba(139,92,246,0.15);border:2px solid #7c3aed;border-radius:12px;
                        padding:24px;text-align:center;margin:0 0 32px;">
              <p style="margin:0 0 8px;color:#94a3b8;font-size:13px;letter-spacing:2px;text-transform:uppercase;">
                Your OTP Code
              </p>
              <p style="margin:0;color:#fff;font-size:42px;font-weight:700;letter-spacing:12px;
                        font-family:monospace;">{otp}</p>
            </div>
            <p style="color:#64748b;font-size:13px;margin:0;">
              If you did not request this code, you can safely ignore this email.
              <br>Do not share this code with anyone.
            </p>
          </td>
        </tr>
        <tr>
          <td style="padding:20px 40px;border-top:1px solid rgba(255,255,255,0.05);text-align:center;">
            <p style="margin:0;color:#475569;font-size:12px;">
              © 2026 VocalBharat · All rights reserved
            </p>
          </td>
        </tr>
      </table>
    </td></tr>
  </table>
</body>
</html>"""


async def send_otp_email(to_email: str, otp: str) -> None:
    """Send an OTP email. Falls back to console output if SMTP is not configured."""
    if settings.use_console_email:
        _console_otp(to_email, otp)
        return
    await _smtp_send(to_email, otp)


def _console_otp(to_email: str, otp: str) -> None:
    """Print OTP to terminal — useful for local development without SMTP credentials."""
    logger.info("=" * 60)
    logger.info("📧  OTP EMAIL (console mode — no SMTP configured)")
    logger.info("    To:   %s", to_email)
    logger.info("    OTP:  %s", otp)
    logger.info("    Note: Set SMTP_HOST in .env to send real emails")
    logger.info("=" * 60)


async def _smtp_send(to_email: str, otp: str) -> None:
    """Send OTP via SMTP using aiosmtplib for non-blocking delivery."""
    import aiosmtplib  # lazy import — only needed in production mode

    msg = MIMEMultipart("alternative")
    msg["Subject"] = "Your VocalBharat Login Code"
    msg["From"] = settings.EMAIL_FROM
    msg["To"] = to_email

    plain_text = (
        f"Your VocalBharat login OTP is: {otp}\n\n"
        "This code expires in 10 minutes. Do not share it with anyone."
    )
    msg.attach(MIMEText(plain_text, "plain"))
    msg.attach(MIMEText(_build_otp_html(otp, to_email), "html"))

    try:
        await aiosmtplib.send(
            msg,
            hostname=settings.SMTP_HOST,
            port=settings.SMTP_PORT,
            username=settings.SMTP_USER or None,
            password=settings.SMTP_PASSWORD or None,
            start_tls=True,
        )
        logger.info("OTP email sent to %s", to_email)
    except Exception as exc:
        logger.error("Failed to send OTP email to %s: %s", to_email, exc)
        # Do not raise — caller will handle missing email gracefully
        raise
