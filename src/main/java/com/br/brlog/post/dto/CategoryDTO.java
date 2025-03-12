package com.br.brlog.post.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class CategoryDTO {
    private String id;
    private String name;
    private String description;
    private String icon;
    private String color;
    private int sortOrder;
}