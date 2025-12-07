"""Initial schema

Revision ID: 0001_initial
Revises: 
Create Date: 2024-01-01 00:00:00
"""
from alembic import op
import sqlalchemy as sa


revision = '0001_initial'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        'user',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('telegram_id', sa.BigInteger(), nullable=False),
        sa.Column('username', sa.String(length=255), nullable=True),
        sa.Column('language', sa.String(length=8), nullable=False, server_default='ru'),
        sa.Column('citizenship_code', sa.String(length=8), nullable=True),
        sa.Column('citizenship_name', sa.String(length=255), nullable=True),
        sa.Column('phone', sa.String(length=32), nullable=True),
        sa.Column('notification_window_start', sa.Time(), nullable=True),
        sa.Column('notification_window_end', sa.Time(), nullable=True),
        sa.Column('is_admin', sa.Boolean(), nullable=False, server_default=sa.text('false')),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()')),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('telegram_id')
    )
    op.create_index(op.f('ix_user_telegram_id'), 'user', ['telegram_id'], unique=False)

    op.create_table(
        'documenttype',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('code', sa.String(length=64), nullable=False),
        sa.Column('name_ru', sa.String(length=255), nullable=False),
        sa.Column('name_en', sa.String(length=255), nullable=False),
        sa.Column('is_active', sa.Boolean(), nullable=False, server_default=sa.text('true')),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('code')
    )
    op.create_index(op.f('ix_documenttype_code'), 'documenttype', ['code'], unique=False)

    op.create_table(
        'userdocument',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('document_type_id', sa.Integer(), nullable=False),
        sa.Column('current_expiry_date', sa.Date(), nullable=True),
        sa.Column('entry_date', sa.Date(), nullable=True),
        sa.Column('submitted_for_extension', sa.Boolean(), nullable=False, server_default=sa.text('false')),
        sa.Column('extension_expiry_date', sa.Date(), nullable=True),
        sa.Column('notifications_enabled', sa.Boolean(), nullable=False, server_default=sa.text('true')),
        sa.Column('last_notification_at', sa.DateTime(timezone=True), nullable=True),
        sa.Column('final_reminder_sent', sa.Boolean(), nullable=False, server_default=sa.text('false')),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()')),
        sa.ForeignKeyConstraint(['document_type_id'], ['documenttype.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['user_id'], ['user.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_userdocument_user_id'), 'userdocument', ['user_id'], unique=False)

    op.create_table(
        'userdocumenthistory',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('user_document_id', sa.Integer(), nullable=False),
        sa.Column('old_expiry_date', sa.Date(), nullable=True),
        sa.Column('new_expiry_date', sa.Date(), nullable=True),
        sa.Column('changed_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()')),
        sa.Column('changed_by', sa.String(length=64), nullable=False, server_default='user'),
        sa.Column('comment', sa.String(length=255), nullable=True),
        sa.ForeignKeyConstraint(['user_document_id'], ['userdocument.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_userdocumenthistory_user_document_id'), 'userdocumenthistory', ['user_document_id'], unique=False)


def downgrade() -> None:
    op.drop_index(op.f('ix_userdocumenthistory_user_document_id'), table_name='userdocumenthistory')
    op.drop_table('userdocumenthistory')
    op.drop_index(op.f('ix_userdocument_user_id'), table_name='userdocument')
    op.drop_table('userdocument')
    op.drop_index(op.f('ix_documenttype_code'), table_name='documenttype')
    op.drop_table('documenttype')
    op.drop_index(op.f('ix_user_telegram_id'), table_name='user')
    op.drop_table('user')
