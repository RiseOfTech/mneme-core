package io.github.ifamneme.mneme_core.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import io.github.ifamneme.mneme_core.domain.Card;
import io.github.ifamneme.mneme_core.domain.CardId;
import io.github.ifamneme.mneme_core.domain.CardRepository;
import io.github.ifamneme.mneme_core.domain.UserId;

@Repository
class JpaCardRepository implements CardRepository {

	private final CardJpaRepository jpaRepository;
	private final CardMapper mapper;

	JpaCardRepository(CardJpaRepository jpaRepository, CardMapper mapper) {
		this.jpaRepository = jpaRepository;
		this.mapper = mapper;
	}

	@Override
	public Card save(Card card) {
		CardEntity saved = jpaRepository.save(mapper.toEntity(card));
		return mapper.toDomain(saved);
	}

	@Override
	public Optional<Card> findByIdAndOwner(CardId id, UserId owner) {
		return jpaRepository.findByIdAndOwnerId(id.value(), owner.value()).map(mapper::toDomain);
	}

	@Override
	public List<Card> findAllByIdsAndOwner(Collection<CardId> ids, UserId owner) {
		List<UUID> uuids = ids.stream().map(CardId::value).toList();
		return jpaRepository.findByIdInAndOwnerId(uuids, owner.value()).stream().map(mapper::toDomain).toList();
	}

	@Override
	public List<Card> findAllByOwner(UserId owner) {
		return jpaRepository.findByOwnerIdOrderByCreatedAtDesc(owner.value()).stream().map(mapper::toDomain).toList();
	}

	@Override
	public void deleteByIdAndOwner(CardId id, UserId owner) {
		jpaRepository.deleteByIdAndOwnerId(id.value(), owner.value());
	}

}
