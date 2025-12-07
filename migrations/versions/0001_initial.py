from __future__ import annotations

from alembic import op
import sqlalchemy as sa

revision = '0001_initial'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        'user',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('telegram_id', sa.Integer(), nullable=False, unique=True, index=True),
        sa.Column('username', sa.String(length=255), nullable=True),
        sa.Column('language', sa.String(length=5), nullable=False, server_default='ru'),
        sa.Column('citizenship_code', sa.String(length=10), nullable=True),
        sa.Column('citizenship_name', sa.String(length=255), nullable=True),
        sa.Column('phone', sa.String(length=32), nullable=True),
        sa.Column('notification_window_start', sa.Time(), nullable=True),
        sa.Column('notification_window_end', sa.Time(), nullable=True),
        sa.Column('is_admin', sa.Boolean(), nullable=False, server_default=sa.false()),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
    )

    op.create_table(
        'documenttype',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('code', sa.String(length=50), nullable=False, unique=True),
        sa.Column('name_ru', sa.String(length=255), nullable=False),
        sa.Column('name_en', sa.String(length=255), nullable=False),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default=sa.true()),
    )

    op.create_table(
        'userdocument',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('user_id', sa.Integer(), sa.ForeignKey('user.id', ondelete='CASCADE'), nullable=False, index=True),
        sa.Column('document_type_id', sa.Integer(), sa.ForeignKey('documenttype.id', ondelete='CASCADE'), nullable=False),
        sa.Column('current_expiry_date', sa.Date(), nullable=True),
        sa.Column('entry_date', sa.Date(), nullable=True),
        sa.Column('submitted_for_extension', sa.Boolean(), nullable=False, server_default=sa.false()),
        sa.Column('extension_expiry_date', sa.Date(), nullable=True),
        sa.Column('notifications_enabled', sa.Boolean(), nullable=False, server_default=sa.true()),
        sa.Column('last_notification_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('final_reminder_sent', sa.Boolean(), nullable=False, server_default=sa.false()),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
    )

    op.create_table(
        'userdocumenthistory',
        sa.Column('id', sa.Integer(), primary_key=True),
        sa.Column('user_document_id', sa.Integer(), sa.ForeignKey('userdocument.id', ondelete='CASCADE'), nullable=False),
        sa.Column('old_expiry_date', sa.Date(), nullable=True),
        sa.Column('new_expiry_date', sa.Date(), nullable=True),
        sa.Column('changed_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
        sa.Column('changed_by', sa.String(length=32), nullable=False, server_default='user'),
        sa.Column('comment', sa.String(length=255), nullable=True),
    )


def downgrade() -> None:
    op.drop_table('userdocumenthistory')
    op.drop_table('userdocument')
    op.drop_table('documenttype')
    op.drop_table('user')
