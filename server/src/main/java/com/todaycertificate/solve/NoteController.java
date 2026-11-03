package com.todaycertificate.solve;

import com.todaycertificate.common.CurrentMember;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** PUT/DELETE는 멱등: 몇 번을 보내도 결과가 같다 (토글이 아니라 "켜기", "끄기"). */
@RestController
public class NoteController {

    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    @PutMapping("/api/questions/{id}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void bookmark(@PathVariable Long id, Authentication auth) {
        service.bookmark(CurrentMember.id(auth), id);
    }

    @DeleteMapping("/api/questions/{id}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unbookmark(@PathVariable Long id, Authentication auth) {
        service.unbookmark(CurrentMember.id(auth), id);
    }

    @GetMapping("/api/me/bookmarks")
    List<NoteService.NoteItem> bookmarks(Authentication auth) {
        return service.bookmarks(CurrentMember.id(auth));
    }

    @GetMapping("/api/me/wrong-notes")
    List<NoteService.NoteItem> wrongNotes(Authentication auth) {
        return service.wrongNotes(CurrentMember.id(auth));
    }
}
