package com.example.ec.web;

import com.example.ec.domain.Tour;
import com.example.ec.domain.TourRating;
import com.example.ec.domain.TourRatingPk;
import com.example.ec.repo.TourRatingRepository;
import com.example.ec.repo.TourRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.AbstractMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Created by Ext-MayukhG on 7/31/2018.
 */
@RestController
@RequestMapping(path = "/tours/{tourId}/ratings")
public class TourRatingController {
    private TourRatingRepository tourRatingRepository;
    private TourRepository tourRepository;

    @Autowired
    public TourRatingController(TourRatingRepository tourRatingRepository, TourRepository tourRepository) {
        this.tourRatingRepository = tourRatingRepository;
        this.tourRepository = tourRepository;
    }

    protected TourRatingController() {
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public void createTourRating(@PathVariable(value = "tourId") int tourId, @RequestBody @Validated RatingDto ratingDto) {
        Tour tour = verifyTour(tourId);
        tourRatingRepository.save(new TourRating(new TourRatingPk(tour, ratingDto.getCustomerId()), ratingDto.getScore(),
                ratingDto.getComment()));
    }

    private Tour verifyTour(int tourId) throws NoSuchElementException {
        Tour tour = tourRepository.findOne(tourId);
        if(tour == null) {
            throw new NoSuchElementException("Tour does not exist" + tourId);
        }
        return tour;
    }

    @RequestMapping(method = RequestMethod.GET)
    public Page<RatingDto> getAllRatingsForTour(@PathVariable(value = "tourId")int tourId, Pageable pageable) {
        verifyTour(tourId);
        /*return tourRatingRepository.findByPkTourId(tourId).stream().map(tourRating -> toDto(tourRating))
                .collect(Collectors.toList());*/
        Page<TourRating> tourRatingPage = tourRatingRepository.findByPkTourId(tourId, pageable);
        List<RatingDto> ratingDtoList = tourRatingPage.getContent().stream().map(tourRating -> toDto(tourRating)).collect(Collectors.toList());
        return new PageImpl<RatingDto>(ratingDtoList, pageable, tourRatingPage.getTotalPages());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/average")
    public AbstractMap.SimpleEntry<String, Double> getAverage(@PathVariable(value = "tourId")int tourId) {
        verifyTour(tourId);
        List<TourRating> tourRatingList = tourRatingRepository.findByPkTourId(tourId);
        OptionalDouble average = tourRatingList.stream().mapToInt(TourRating::getScore).average();
        return new AbstractMap.SimpleEntry<String, Double>("average", average.isPresent()?average.getAsDouble():
                null);
    }

    private TourRating verifyTourRating(int tourId, int customerId) throws NoSuchElementException {
        TourRating tourRating = tourRatingRepository.findByPkTourIdAndPkCustomerId(tourId, customerId);
        if(tourRating == null) {
            throw new NoSuchElementException("Tour-rating pair for request(" + tourId + " for customer" + customerId);
        }
        return tourRating;
    }

    @RequestMapping(method = RequestMethod.PUT)
    public RatingDto updateWithPut(@PathVariable(value = "tourId") int tourId, @RequestBody @Validated RatingDto ratingDto) {
        TourRating tourRating = verifyTourRating(tourId, ratingDto.getCustomerId());
        tourRating.setScore(ratingDto.getScore());
        tourRating.setComment(ratingDto.getComment());
        return toDto(tourRatingRepository.save(tourRating));
    }

    @RequestMapping(method = RequestMethod.PATCH)
    public RatingDto updateWithPatch(@PathVariable(value = "tourId") int tourId, @RequestBody @Validated RatingDto ratingDto) {
        TourRating tourRating = verifyTourRating(tourId, ratingDto.getCustomerId());
        if(ratingDto.getScore() != null) {
            tourRating.setScore(ratingDto.getScore());
        }
        if(ratingDto.getComment() != null) {
            tourRating.setComment(ratingDto.getComment());
        }
        return toDto(tourRatingRepository.save(tourRating));
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/{customerId}")
    public void delete(@PathVariable(value = "tourId") int tourId, @PathVariable(value = "customerId") int customerId) {
        TourRating tourRating = verifyTourRating(tourId, customerId);
        tourRatingRepository.delete(tourRating);
    }

    private RatingDto toDto(TourRating tourRating) {
        return new RatingDto(tourRating.getScore(), tourRating.getComment(), tourRating.getPk().getCustomerId());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoSuchElementException.class)
    public String return400(NoSuchElementException ex) {
        return ex.getMessage();
    }
}