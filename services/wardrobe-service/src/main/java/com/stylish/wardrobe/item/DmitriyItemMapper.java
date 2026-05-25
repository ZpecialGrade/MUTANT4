package com.stylish.wardrobe.item;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stylish.wardrobe.item.dto.DmitriyItemResponse;

@Mapper(componentModel = "spring")
public interface DmitriyItemMapper {
	@Mapping(target = "imageObjectKey", source = "imageObjectKey")
	DmitriyItemResponse toResponse(DmitriyItemEntity entity);
}

