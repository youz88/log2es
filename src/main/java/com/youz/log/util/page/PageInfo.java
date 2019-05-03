package com.youz.log.util.page;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo<T> implements Serializable {

    private long total;
    private List<T> list;

    @SuppressWarnings("unchecked")
    public static <T> PageInfo<T> returnList(List<T> list) {
        if (list instanceof com.github.miemiedev.mybatis.paginator.domain.PageList) {
            return new PageInfo(((com.github.miemiedev.mybatis.paginator.domain.PageList) list).getPaginator().getTotalCount(), new ArrayList(list));
        } else {
            return new PageInfo(0, list);
        }
    }
}
