package com.springboot.springsecurity.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.springboot.springsecurity.controller.dto.CreateTweetDto;
import com.springboot.springsecurity.controller.dto.FeedDto;
import com.springboot.springsecurity.controller.dto.FeedItemDto;
import com.springboot.springsecurity.entity.Role;
import com.springboot.springsecurity.entity.Tweet;
import com.springboot.springsecurity.repository.TweetRepository;
import com.springboot.springsecurity.repository.UserRepository;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
public class TweetController {

    private final TweetRepository tweetRepository;
    private final UserRepository userRepository;

    public TweetController(TweetRepository tweetRepository, UserRepository userRepository) {
        this.tweetRepository = tweetRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/feed")
    public ResponseEntity<FeedDto> getFeed(@RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            JwtAuthenticationToken token) {

        var tweets = tweetRepository.findAll(
                PageRequest.of(page, pageSize, Sort.Direction.DESC, "creationTimeStamp"))
                .map(tweet -> new FeedItemDto(tweet.getTweetId(), tweet.getContent(), tweet.getUser().getUsername()));

        return ResponseEntity.ok(
                new FeedDto(tweets.getContent(), page, pageSize, tweets.getTotalPages(), tweets.getTotalElements()));
    }

    @PostMapping("/tweets")
    public ResponseEntity<Void> createTweet(@RequestBody CreateTweetDto dto, JwtAuthenticationToken token) {

        var user = userRepository.findById(UUID.fromString(token.getName()));

        var tweet = new Tweet();
        tweet.setUser(user.get());
        tweet.setContent(dto.content());

        tweetRepository.save(tweet);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/tweets/{id}")
    public ResponseEntity<Void> deleteTweet(@PathVariable("id") Long tweetId, JwtAuthenticationToken token) {

        var user = userRepository.findById(UUID.fromString(token.getName()));
        var tweet = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var isAdmin = user.get().getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(Role.Values.ADMIN.name()));

        if (isAdmin || tweet.getUser().getUserId().equals(UUID.fromString(token.getName()))) {
            tweetRepository.delete(tweet);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok().build();
    }

}
