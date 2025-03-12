package com.br.brlog.post.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ContributorDTO {
    private String id;
    private String name;
    private String avatar;
    private int contributions;
}