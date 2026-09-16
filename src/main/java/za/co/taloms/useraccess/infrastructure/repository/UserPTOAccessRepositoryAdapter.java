package za.co.taloms.useraccess.infrastructure.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import za.co.taloms.useraccess.domain.entity.UserPTOAccess;
import za.co.taloms.useraccess.domain.repository.UserPTOAccessRepositoryPort;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserPTOAccessRepositoryAdapter implements UserPTOAccessRepositoryPort {

    private final UserPTOAccessJpaRepository jpaRepository;

    @Override
    public UserPTOAccess save(UserPTOAccess access) {
        return jpaRepository.save(access);
    }

    @Override
    public Optional<UserPTOAccess> findByUserIdAndPtoId(Long userId, Long ptoId) {
        return jpaRepository.findByUserIdAndPtoId(userId, ptoId);
    }

    @Override
    public List<UserPTOAccess> findAllByPtoId(Long ptoId) {
        return jpaRepository.findAllByPtoId(ptoId);
    }

    @Override
    public List<UserPTOAccess> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserId(userId);
    }

    @Override
    public boolean existsByUserIdAndPtoId(Long userId, Long ptoId) {
        return jpaRepository.existsByUserIdAndPtoId(userId, ptoId);
    }

    @Override
    public void deleteByUserIdAndPtoId(Long userId, Long ptoId) {
        jpaRepository.deleteByUserIdAndPtoId(userId, ptoId);
    }
}