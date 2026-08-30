package com.roost.channel;

/**
 * What a channel carries. {@code TEXT} channels hold messages (Phase 1);
 * {@code VOICE} channels host LiveKit sessions (Phase 2). Persisted as its name
 * ({@code EnumType.STRING}).
 */
public enum ChannelType {
    TEXT,
    VOICE
}
