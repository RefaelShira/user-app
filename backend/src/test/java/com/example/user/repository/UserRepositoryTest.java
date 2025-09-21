package com.example.user.repository;

import com.example.user.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;

@DataJpaTest
@Sql(scripts = "classpath:schema.sql")
public class UserRepositoryTest {
    @Autowired
    private UserRepository repo;

    private UserEntity activeIsrael;
    private UserEntity inactiveEli;

    @BeforeEach
    void setUp() {
        repo.deleteAll();
        activeIsrael = repo.save(getUserEntity(
                "Israel", "Israeli", "israel@example.com",
                true,  0L
        ));
        inactiveEli = repo.save(getUserEntity(
                "ELi", "Copter", "eli@copter.com",
                false, 0L
        ));
        repo.flush();
    }

    @Test
    @DisplayName("findByEmailIgnoreCase")
    void findByEmailIgnoreCaseTest() {
        Optional<UserEntity> found = repo.findByEmailIgnoreCase("israel@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("israel@example.com");
    }
    @Test
    @DisplayName("search")
    void searchTest() {
        Page<UserEntity> page = repo.search("Israeli", true, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).isActive()).isTrue();
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("israel@example.com");
    }
    @Test
    @DisplayName("countUsersCreatedSince")
    void countUsersCreatedSinceTest() {
        var since = Instant.now().minusSeconds(60L * 60 * 24);
        Page<UserEntity> page = repo.search(null, false, PageRequest.of(0, 10));
        long cnt = repo.countUsersCreatedSince(since);
        assertThat(cnt).isEqualTo(2);
    }

    @Test
    @DisplayName("DataIntegrityViolationException")
    void uniqueEmailTest() {
        var duplicate = getUserEntity("Israel", "Israeli", "israel@example.com",
                true, 0L);
        repo.save(duplicate);
        assertThatThrownBy(repo::flush)
                .isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test
    @DisplayName("uniqueEmailCaseInsensitive")
    void uniqueEmailCaseInsensitiveTest() {
        repo.deleteAll(); repo.flush();
        repo.save(getUserEntity("Israel", "Israeli", "israel@example.com",
                true, 0L));
        repo.flush();

        repo.save(getUserEntity("Israel", "Israeli", "ISRAEL@example.com",
                true, 0L));
        assertThatThrownBy(repo::flush).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test
    void search_pagination_and_ordering() {
        repo.deleteAll();
        repo.flush();
        for (int i=0;i<15;i++) {
            repo.save(getUserEntity("U"+i,"L","u"+i+"@ex.com", true, 0L));
        }
        repo.flush();

        var page0 = repo.search("", true, PageRequest.of(0, 5));
        var page1 = repo.search("", true, PageRequest.of(1, 5));
        assertThat(page0.getSize()).isEqualTo(5);
        assertThat(page0.getTotalElements()).isEqualTo(15);
        assertThat(page1.getContent()).doesNotContainAnyElementsOf(page0.getContent());
    }



    private static UserEntity getUserEntity(
            String firstName, String lastName, String email,
            boolean active, Long version) {
        return UserEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .active(active)
                .version(version)
                .passwordHash("passwordHash")
                .build();
    }
}
