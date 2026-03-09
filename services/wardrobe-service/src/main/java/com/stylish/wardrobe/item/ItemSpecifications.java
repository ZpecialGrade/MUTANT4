package com.stylish.wardrobe.item;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

public final class ItemSpecifications {
	private ItemSpecifications() {
	}

	public static Specification<ItemEntity> profileId(UUID profileId) {
		return (root, query, cb) -> cb.equal(root.get("profile").get("id"), profileId);
	}

	public static Specification<ItemEntity> type(ItemType type) {
		return (root, query, cb) -> cb.equal(root.get("type"), type);
	}

	public static Specification<ItemEntity> colorEqualsIgnoreCase(String color) {
		return (root, query, cb) -> cb.equal(cb.lower(root.get("color")), color.toLowerCase());
	}

	public static Specification<ItemEntity> nameLikeIgnoreCase(String nameLike) {
		String pattern = "%" + nameLike.toLowerCase() + "%";
		return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
	}
}

