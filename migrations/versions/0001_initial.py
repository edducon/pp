"""Initial schema

Revision ID: 0001_initial
Revises:
Create Date: 2024-01-01 00:00:00
"""

from alembic import op
import sqlalchemy as sa

from app.enums import MigrationCardStatus, MigrationEvent

revision = '0001_initial'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Ensure enums are recreated with the expected lowercase values even if a stale type
    # was left behind from earlier iterations of the project.
    migration_card_status = sa.Enum(
        MigrationCardStatus,
        name='migrationcardstatus',
        create_type=False,
    )
    migration_event = sa.Enum(
        MigrationEvent,
        name='migrationevent',
        create_type=False,
    )

    op.execute("DROP TYPE IF EXISTS migrationcardstatus CASCADE")
    op.execute("DROP TYPE IF EXISTS migrationevent CASCADE")

    migration_card_status.create(op.get_bind(), checkfirst=False)
    migration_event.create(op.get_bind(), checkfirst=False)

    op.create_table(
        'countries',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('code3', sa.String(length=3), nullable=False),
        sa.Column('name_ru', sa.String(length=128), nullable=False),
        sa.Column('name_en', sa.String(length=128), nullable=False),
        sa.Column('aliases', sa.JSON(), nullable=False, server_default=sa.text("'[]'::jsonb")),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('code3')
    )

    op.create_table(
        'users',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('telegram_id', sa.BigInteger(), nullable=False),
        sa.Column('language', sa.String(length=8), nullable=False, server_default='ru'),
        sa.Column('first_name', sa.String(length=128), nullable=False),
        sa.Column('last_name', sa.String(length=128), nullable=False),
        sa.Column('patronymic', sa.String(length=128), nullable=True),
        sa.Column('citizenship_id', sa.Integer(), nullable=True),
        sa.Column('phone_from_telegram', sa.String(length=32), nullable=True),
        sa.Column('manual_phone', sa.String(length=32), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()'), server_onupdate=sa.text('now()')),
        sa.ForeignKeyConstraint(['citizenship_id'], ['countries.id'], ),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('telegram_id')
    )
    op.create_index(op.f('ix_users_telegram_id'), 'users', ['telegram_id'], unique=False)

    op.create_table(
        'admins',
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('granted_by', sa.BigInteger(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()')),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('user_id')
    )

    op.create_table(
        'migration_cards',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('expires_at', sa.Date(), nullable=False),
        sa.Column('status', migration_card_status, nullable=False, server_default=MigrationCardStatus.ACTIVE.value),
        sa.Column('paused_by_user', sa.Boolean(), nullable=False, server_default=sa.text('false')),
        sa.Column('last_notified_on', sa.Date(), nullable=True),
        sa.Column('needs_travel_confirmation', sa.Boolean(), nullable=False, server_default=sa.text('false')),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()')),
        sa.Column('updated_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()'), server_onupdate=sa.text('now()')),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id'),
        sa.UniqueConstraint('user_id', name='unique_active_card')
    )
    op.create_index('idx_migration_cards_expires', 'migration_cards', ['expires_at'], unique=False)

    op.create_table(
        'migration_card_history',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('migration_card_id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('event', migration_event, nullable=False),
        sa.Column('previous_expires_at', sa.Date(), nullable=True),
        sa.Column('expires_at', sa.Date(), nullable=True),
        sa.Column('in_russia', sa.Boolean(), nullable=True),
        sa.Column('note', sa.String(length=255), nullable=True),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False, server_default=sa.text('now()')),
        sa.ForeignKeyConstraint(['migration_card_id'], ['migration_cards.id'], ondelete='CASCADE'),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )


def downgrade() -> None:
    op.drop_table('migration_card_history')
    op.drop_index('idx_migration_cards_expires', table_name='migration_cards')
    op.drop_table('migration_cards')
    op.drop_table('admins')
    op.drop_index(op.f('ix_users_telegram_id'), table_name='users')
    op.drop_table('users')
    op.drop_table('countries')
    op.execute("DROP TYPE IF EXISTS migrationcardstatus")
    op.execute("DROP TYPE IF EXISTS migrationevent")
