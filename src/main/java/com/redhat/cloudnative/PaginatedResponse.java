package com.redhat.cloudnative;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Generic paginated response wrapper that includes metadata about pagination.
 * 
 * @param <T> The type of data in the response
 */
@Schema(description = "Paginated response with metadata")
public class PaginatedResponse<T> {

    @Schema(description = "List of items in the current page")
    private List<T> data;

    @Schema(description = "Total number of items across all pages", example = "100")
    private long total;

    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;

    @Schema(description = "Number of items per page", example = "20")
    private int size;

    @Schema(description = "Total number of pages", example = "5")
    private int totalPages;

    @Schema(description = "Whether there is a next page", example = "true")
    private boolean hasNext;

    @Schema(description = "Whether there is a previous page", example = "false")
    private boolean hasPrevious;

    public PaginatedResponse() {
    }

    public PaginatedResponse(List<T> data, long total, int page, int size) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.size = size;
        this.totalPages = calculateTotalPages(total, size);
        this.hasNext = page < totalPages - 1;
        this.hasPrevious = page > 0;
    }

    private int calculateTotalPages(long total, int size) {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) total / size);
    }

    // Getters and Setters
    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    /**
     * Builder method to create a PaginatedResponse from a Panache query result
     */
    public static <T> PaginatedResponse<T> of(List<T> data, long total, int page, int size) {
        return new PaginatedResponse<>(data, total, page, size);
    }
}