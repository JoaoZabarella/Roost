package com.roost;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.roost.channel.Channel;
import com.roost.channel.ChannelRepository;
import com.roost.channel.ChannelType;
import com.roost.chat.Message;
import com.roost.chat.MessageRepository;
import com.roost.server.Server;
import com.roost.server.ServerMember;
import com.roost.server.ServerMemberRepository;
import com.roost.server.ServerMemberRole;
import com.roost.server.ServerRepository;
import com.roost.user.User;
import com.roost.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

/**
 * Proves the Phase 1 schema (Flyway V2) and the JPA mappings agree: Hibernate's
 * {@code ddl-auto: validate} passes against the migrated schema, the graph
 * persists end to end, and the key constraints/queries behave. Runs against a
 * real Postgres (Testcontainers) because we rely on Postgres-specific types
 * (uuid, timestamptz) and {@code gen_random_uuid()}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DomainPersistenceTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository users;

    @Autowired
    private ServerRepository servers;

    @Autowired
    private ServerMemberRepository members;

    @Autowired
    private ChannelRepository channels;

    @Autowired
    private MessageRepository messages;

    @Test
    void persistsFullGraphAndAssignsIds() {
        User owner = users.save(new User("neo", "neo@roost.dev", "hash", "Neo"));
        Server server = servers.save(new Server("The Construct", owner));
        ServerMember membership =
            members.save(new ServerMember(server, owner, ServerMemberRole.OWNER));
        Channel channel =
            channels.save(new Channel(server, "general", ChannelType.TEXT, 0));
        Message message = messages.save(new Message(channel, owner, "hello world"));
        em.flush(); // force the inserts so @CreationTimestamp is populated

        // gen_random_uuid()/Hibernate assigned every primary key.
        assertThat(owner.getId()).isNotNull();
        assertThat(server.getId()).isNotNull();
        assertThat(membership.getId()).isNotNull();
        assertThat(channel.getId()).isNotNull();
        assertThat(message.getId()).isNotNull();

        // @CreationTimestamp populated created_at.
        assertThat(owner.getCreatedAt()).isNotNull();
        assertThat(message.getCreatedAt()).isNotNull();
        assertThat(message.getEditedAt()).isNull();
    }

    @Test
    void looksUpUserByUniqueColumns() {
        users.save(new User("trinity", "trinity@roost.dev", "hash", "Trinity"));

        assertThat(users.findByUsername("trinity")).isPresent();
        assertThat(users.existsByEmail("trinity@roost.dev")).isTrue();
        assertThat(users.findByEmail("nobody@roost.dev")).isEmpty();
    }

    @Test
    void rejectsDuplicateMembership() {
        User user = users.save(new User("dozer", "dozer@roost.dev", "hash", "Dozer"));
        Server server = servers.save(new Server("Nebuchadnezzar", user));
        members.save(new ServerMember(server, user, ServerMemberRole.OWNER));

        // Same (server_id, user_id) violates uq_server_members_server_user.
        assertThatThrownBy(() ->
                members.saveAndFlush(new ServerMember(server, user, ServerMemberRole.MEMBER)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void paginatesChannelMessages() {
        User user = users.save(new User("tank", "tank@roost.dev", "hash", "Tank"));
        Server server = servers.save(new Server("Zion", user));
        Channel channel = channels.save(new Channel(server, "ops", ChannelType.TEXT, 0));
        messages.save(new Message(channel, user, "one"));
        messages.save(new Message(channel, user, "two"));
        messages.save(new Message(channel, user, "three"));
        em.flush();
        em.clear();

        List<Message> firstPage = messages.findByChannelIdOrderByCreatedAtDescIdDesc(
            channel.getId(), PageRequest.of(0, 2));
        List<Message> secondPage = messages.findByChannelIdOrderByCreatedAtDescIdDesc(
            channel.getId(), PageRequest.of(1, 2));

        assertThat(firstPage).hasSize(2);
        assertThat(secondPage).hasSize(1);
        assertThat(collectIds(firstPage, secondPage)).hasSize(3);
    }

    private static List<UUID> collectIds(List<Message> a, List<Message> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream())
            .map(Message::getId)
            .distinct()
            .toList();
    }
}
