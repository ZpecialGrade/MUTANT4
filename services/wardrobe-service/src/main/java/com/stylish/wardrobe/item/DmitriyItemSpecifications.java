package com.stylish.wardrobe.item;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

public final class DmitriyItemSpecifications {
	private DmitriyItemSpecifications() {
	}

	public static Specification<DmitriyItemEntity> profileId(UUID profileId) {
		return (root, query, cb) -> cb.equal(root.get("profile").get("id"), profileId);
	}

	public static Specification<DmitriyItemEntity> type(DmitriyItemType type) {
		return (root, query, cb) -> cb.equal(root.get("type"), type);
	}

	public static Specification<DmitriyItemEntity> colorEqualsIgnoreCase(String color) {
		return (root, query, cb) -> cb.equal(cb.lower(root.get("color")), color.toLowerCase());
	}

	public static Specification<DmitriyItemEntity> nameLikeIgnoreCase(String nameLike) {
		String pattern = "%" + nameLike.toLowerCase() + "%";
		return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
	}
}

