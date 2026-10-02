package team.magic.flute.hercules.common.http;

import lombok.ToString;

import java.util.ArrayList;
import java.util.List;


@ToString
public class PageData<T> {

    public final static Integer DEFAULT_PAGE_NUMBER = 1;
    public final static Integer DEFAULT_PAGE_SIZE = 20;
    public final static Long DEFAULT_TOTAL = 0L;

    /**
     * current-page [1, Integer.MAX)
     */
    private Integer page;
    /**
     * current-page-size
     */
    private Integer size;
    /**
     * total-page-num
     */
    private Integer totalPage;
    /**
     * total-data-num
     */
    private Long total;

    private List<T> list;

    private Long offset;

    public PageData() {
    }

    public PageData(Integer page, Integer size, Long total, List<T> list) {
        this.page = page;
        this.size = size;
        this.total = total;
        this.list = list;
    }

    public Integer getPage() {
        if (null == this.page) {
            this.page = DEFAULT_PAGE_NUMBER;
        }
        return page;
    }

    public PageData setPage(Integer page) {
        this.page = page;
        return this;
    }

    public Integer getSize() {
        if (this.size == null) {
            this.size = DEFAULT_PAGE_SIZE;
        }
        return size;
    }

    public PageData setSize(Integer size) {
        this.size = size;
        return this;
    }

    public Integer getTotalPage() {
        if (this.total % this.size == 0) {
            this.totalPage = Math.toIntExact(this.total / this.size);
        } else {
            this.totalPage = Math.toIntExact(this.total / this.size + 1);
        }
        return totalPage;
    }

    public PageData setTotalPage(Integer totalPage) {
        this.totalPage = totalPage;
        return this;
    }

    public Long getTotal() {
        if (this.total == null) {
            total = DEFAULT_TOTAL;
        }
        return total;
    }

    public PageData setTotal(Long total_size) {
        this.total = total_size;
        return this;
    }

    public List<T> getList() {
        if (this.list == null) {
            this.list = new ArrayList<>();
        }
        return list;
    }

    public PageData setList(List<T> list) {
        this.list = list;
        return this;
    }

    public Long getOffset() {
        if (offset == null) {
            offset = (long) (getPage() - 1) * getSize();
        }
        return offset;
    }

    public PageData setOffset(Long offset) {
        this.offset = offset;
        return this;
    }
}

