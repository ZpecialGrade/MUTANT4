package com.stylish.wardrobe.item;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.stylish.wardrobe.item.dto.ItemResponse;

@Mapper(componentModel = "spring")
public interface ItemMapper {
	@Mapping(target = "imageObjectKey", source = "imageObjectKey")
	ItemResponse toResponse(ItemEntity entity);
}

