from __future__ import annotations

from alembic import op
import sqlalchemy as sa

revision = '0001_initial'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        'users',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('telegram_id', sa.Integer(), nullable=False, unique=True, index=True),
        sa.Column('language', sa.String(length=5), nullable=False),
        sa.Column('citizenship_code', sa.String(length=10), nullable=False),
        sa.Column('phone', sa.String(length=20), nullable=True),
        sa.Column('notification_time_start', sa.Time(), nullable=False),
        sa.Column('notification_time_end', sa.Time(), nullable=False),
        sa.Column('is_admin', sa.Boolean(), nullable=False, server_default=sa.false()),
    )

    op.create_table(
        'document_types',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('code', sa.String(length=50), nullable=False, unique=True),
        sa.Column('name_ru', sa.String(length=255), nullable=False),
        sa.Column('name_en', sa.String(length=255), nullable=False),
        sa.Column('rules', sa.JSON(), nullable=True),
    )

    op.create_table(
        'user_documents',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('user_id', sa.Integer(), sa.ForeignKey('users.id', ondelete='CASCADE'), nullable=False, index=True),
        sa.Column('document_type_id', sa.Integer(), sa.ForeignKey('document_types.id', ondelete='CASCADE'), nullable=False),
        sa.Column('expiry_date', sa.Date(), nullable=True),
        sa.Column('submitted_for_extension', sa.Boolean(), nullable=False, server_default=sa.false()),
        sa.Column('notifications_enabled', sa.Boolean(), nullable=False, server_default=sa.true()),
        sa.Column('last_notification_at', sa.DateTime(), nullable=True),
    )

    op.create_table(
        'user_document_history',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('document_id', sa.Integer(), sa.ForeignKey('user_documents.id', ondelete='CASCADE'), nullable=False),
        sa.Column('old_expiry_date', sa.Date(), nullable=True),
        sa.Column('new_expiry_date', sa.Date(), nullable=True),
        sa.Column('changed_at', sa.DateTime(), nullable=False, server_default=sa.func.now()),
    )


def downgrade() -> None:
    op.drop_table('user_document_history')
    op.drop_table('user_documents')
    op.drop_table('document_types')
    op.drop_table('users')
