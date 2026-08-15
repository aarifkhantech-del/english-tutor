from sqlalchemy import create_engine, event, text
from sqlalchemy.orm import DeclarativeBase, sessionmaker, Session
from app.core.config import settings

# SQLite needs check_same_thread=False for multithreaded FastAPI
engine = create_engine(
    settings.DATABASE_URL,
    connect_args={"check_same_thread": False} if settings.is_sqlite else {},
    echo=settings.is_dev,
)

# Enable FK enforcement on every SQLite connection
if settings.is_sqlite:
    @event.listens_for(engine, "connect")
    def set_sqlite_pragma(dbapi_conn, _):
        dbapi_conn.execute("PRAGMA foreign_keys=ON")

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass


def init_db():
    """Create tables and apply lightweight column migrations if SQLite database exists."""
    Base.metadata.create_all(bind=engine)
    if settings.is_sqlite:
        with engine.connect() as conn:
            # Check if request_count column exists in users table
            try:
                result = conn.execute(text("PRAGMA table_info(users)"))
                columns = [row[1] for row in result.fetchall()]
                if "request_count" not in columns:
                    conn.execute(text("ALTER TABLE users ADD COLUMN request_count INTEGER DEFAULT 0"))
                    conn.commit()
            except Exception as e:
                pass


def get_db():
    """FastAPI dependency — yields a DB session and ensures cleanup."""
    db: Session = SessionLocal()
    try:
        yield db
    finally:
        db.close()
