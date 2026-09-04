package com.jet.align.calendar;

import com.jet.align.calendar.dto.EventFilter;
import com.jet.align.calendar.dto.EventRequest;
import com.jet.align.calendar.dto.EventResponse;
import com.jet.align.common.response.ApiResponse;
import com.jet.align.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/calendar/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal User user
    ) {
        EventResponse response = eventService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Event created successfully.", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        EventResponse response = eventService.getById(user, id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Event retrieved successfully.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getEvents(
            @AuthenticationPrincipal User user,
            @ModelAttribute EventFilter filter,
            @PageableDefault(size = 20, sort = "startAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<EventResponse> response = eventService.list(user, filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Events retrieved successfully.", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal User user
    ) {
        EventResponse response = eventService.update(user, id, request);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Event updated successfully.", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        eventService.delete(user, id);
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "Event deleted successfully.", null));
    }
}
