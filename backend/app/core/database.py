from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    """Compatibility base for SQLAlchemy models. MongoDB is the active persistence layer."""
    pass


class _NullQuery:
    """Minimal no-op query object for routes that still declare a DB dependency."""

    def __init__(self, model):
        self.model = model

    def filter(self, *args, **kwargs):
        return self

    def order_by(self, *args, **kwargs):
        return self

    def first(self):
        return None

    def all(self):
        return []

    def count(self):
        return 0

    def update(self, *args, **kwargs):
        return 0


class _NullSession:
    """No-op session object used until all routes are migrated to Mongo repositories."""

    def query(self, model):
        return _NullQuery(model)

    def add(self, *args, **kwargs):
        return None

    def flush(self, *args, **kwargs):
        return None

    def commit(self, *args, **kwargs):
        return None

    def refresh(self, *args, **kwargs):
        return None

    def close(self):
        return None


def init_db():
    """Mongo-only app: no relational table bootstrap is needed."""
    return None


def get_db():
    """Compatibility hook for FastAPI dependency injection during the Mongo migration."""
    return _NullSession()
