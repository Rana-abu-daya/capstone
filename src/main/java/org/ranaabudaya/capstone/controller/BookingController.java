package org.ranaabudaya.capstone.controller;

import jakarta.persistence.Access;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.ranaabudaya.capstone.dto.ServicesDTO;
import org.ranaabudaya.capstone.entity.*;
import org.ranaabudaya.capstone.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
public class BookingController {

  BookingService bookingService ;
    UserService userService;
    CleanerService cleanerService;
    CustomerService customerService;
    ServicesService servicesService;

  @Autowired
  public BookingController(ServicesService servicesService,CustomerService customerService,CleanerService cleanerService,BookingService bookingService,   UserService userService){
      this.bookingService= bookingService;
      this.userService=userService;
      this.cleanerService=cleanerService;
      this.customerService=customerService;
      this.servicesService=servicesService;
  }



    @GetMapping("/bookings")
    public String bookingList(Model model, Principal principal) {
        // Get the logged-in user's username (which is the user ID)
        if(principal == null){

            return "redirect:/login";
        }
        String username = principal.getName();
        List<Booking> bookings;
        User user = userService.findUserByEmail(username);

        if(user.hasRole("ROLE_CLIENT")){
            System.out.println("here Client"+principal.toString());
            Customer customer=customerService.findCustomerByUserId(user.getId());
            model.addAttribute("deletedCustomer", customer.isDeleted());
            bookings = bookingService.findBookingByCustomerId(customer.getId());

        }else if(user.hasRole("ROLE_CLEANER")){
            System.out.println("here ROLE_CLEANER"+principal.toString());
            Cleaner cleaner= cleanerService.findByUserId(user.getId());
            bookings=bookingService.findBookingByCleanerId(cleaner.getId());

        }else{
            System.out.println("here admin"+principal.toString());

            bookings = bookingService.getAll();
        }

        // Add the bookings to the model
      model.addAttribute("bookings", bookings);


        return "bookings";
    }
    @GetMapping("/bookings/start/{id}")
    @ResponseBody
    public String[] startBookingbyId(@PathVariable("id") int id, Model model) {
        Booking booking = bookingService.findBookingById(id).get();
        int result=0;
        if(booking != null && booking.getStatus()== Booking.BookingStatus.NEW){

            booking.setStatus(Booking.BookingStatus.IN_PROGRESS);
            bookingService.update(booking);
            result=1;
        }
        //  bookingService.deleteById(id);
        String arr[] = new String[2];
        if(result>0){
            arr[0] = "The booking is started successfully";
            arr[1]= "success";
            return  arr;

        }else {
            arr[0] = "Can't mark the booking as started. It may be not found or not New booking.";
            arr[1]= "danger";
            return  arr;

        }

    }

    @GetMapping("/bookings/done/{id}")
    @ResponseBody
    public String[] doneBookingbyId(@PathVariable("id") int id, Model model) {
        Booking booking = bookingService.findBookingById(id).get();
        int result=0;
        if(booking != null && booking.getStatus()== Booking.BookingStatus.IN_PROGRESS){

            booking.setStatus(Booking.BookingStatus.SUCCESS);
            bookingService.update(booking);
            result=1;
        }
        //  bookingService.deleteById(id);
        String arr[] = new String[2];
        if(result>0){
            arr[0] = "The booking is completed successfully";
            arr[1]= "success";
            return  arr;

        }else {
            arr[0] = "Can't mark the booking as completed. It may be not found or not In progress booking.";
            arr[1]= "danger";
            return  arr;

        }

    }
    @GetMapping("/bookings/delete/{id}")
    @ResponseBody
    public String[] deleteBookingbyId(@PathVariable("id") int id, Model model) {
        Booking booking = bookingService.findBookingById(id).get();
        int result=0;
        if(booking != null && booking.getStatus()== Booking.BookingStatus.NEW){

            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingService.update(booking);
            result=1;
        }
        //  bookingService.deleteById(id);
        String arr[] = new String[2];
        if(result>0){
            arr[0] = "The booking is cancelled successfully";
            arr[1]= "success";
            return  arr;

        }else {
            arr[0] = "Can't cancel the booking. It may be not found or not new booking.";
            arr[1]= "danger";
            return  arr;

        }

    }



    @GetMapping("/bookings/edit-booking/{id}")
    public String editBookingbyId(@PathVariable("id") int id, Model model) {
        Booking booking=bookingService.findBookingById(id).get();
        System.out.println(booking);
        List<Services> services = servicesService.getAllActiveServices();
        model.addAttribute("services", services);
        Instant instant = booking.getDate().toInstant();
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        LocalDate localDate = zdt.toLocalDate();
        System.out.println(localDate);
        List<Cleaner> cleaners = cleanerService.findAvailableCleanersForServiceAndTime(booking.getStartTime(),booking.getHours(),localDate,booking.getService().getId());
        cleaners.add(booking.getCleaner());
        model.addAttribute("cleaners", cleaners);
        model.addAttribute("booking", booking);
        model.addAttribute("id", id);
        return "edit-booking";
    }

    @PostMapping("/bookings/updateBooking/{id}")
    public String updateServices(@PathVariable("id") int id, @Valid @ModelAttribute("booking") Booking booking, BindingResult bindingResult, Model model, RedirectAttributes redirectAttrs) {
        System.out.println(booking.getDate());
        if(bindingResult.hasErrors())

        {  List<Services> services = servicesService.getAllActiveServices();
            model.addAttribute("services", services);
            model.addAttribute("id", id);
            // log.warn("Wrong attempt");
            return "edit-booking";
        }

        Optional<Booking> existbooking = bookingService.findBookingById(id);
        if (existbooking.isPresent() ) {
            if(existbooking.get().getStatus().equals(Booking.BookingStatus.NEW)) {
                // Get old booking
                Booking oldBooking = existbooking.get();

                // If cleaner, time, or hours have changed
                if (!oldBooking.getCleaner().equals(booking.getCleaner()) ||
                        !oldBooking.getStartTime().equals(booking.getStartTime()) || !oldBooking.getDate().equals(booking.getDate()) ||
                        !(oldBooking.getHours() == (booking.getHours()))) {

                    Instant instant = booking.getDate().toInstant();
                    ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
                    LocalDate localDate = zdt.toLocalDate();
                    System.out.println(localDate);
                    // Check if cleaner is available
                    List<Cleaner> availableCleaners = cleanerService.findAvailableCleanersForServiceAndTime(booking.getStartTime(), booking.getHours(), localDate, booking.getService().getId());

                    // If the cleaner is different, check if they are available
                    if (!oldBooking.getCleaner().equals(booking.getCleaner()) && !availableCleaners.contains(booking.getCleaner())) {
                        Instant instant2 = oldBooking.getDate().toInstant();
                        ZonedDateTime zdt2 = instant2.atZone(ZoneId.systemDefault());
                        LocalDate localDate2 = zdt2.toLocalDate();

                        List<Cleaner> cleaners = cleanerService.findAvailableCleanersForServiceAndTime(oldBooking.getStartTime(),oldBooking.getHours(),localDate2,oldBooking.getService().getId());
                        cleaners.add(booking.getCleaner());
                        redirectAttrs.addFlashAttribute("cleaners", cleaners);
                        List<Services> services = servicesService.getAllActiveServices();
                        redirectAttrs.addFlashAttribute("services", services);
                        redirectAttrs.addFlashAttribute("booking", oldBooking);
                        redirectAttrs.addFlashAttribute("message", "The selected cleaner is not available at the requested time.");
                        redirectAttrs.addFlashAttribute("alertType", "alert-danger");
                        return "redirect:/bookings/edit-booking/"+booking.getId();
                    }

                    // If cleaner is the same as old cleaner, check if they would still be available for new time and service
                    if (oldBooking.getCleaner().equals(booking.getCleaner())) {
                        List<Cleaner> availableCleanersForOld = cleanerService.findAvailableCleanersForServiceAndTime(booking.getStartTime(), booking.getHours(), localDate, booking.getService().getId());
                        if (!availableCleanersForOld.contains(booking.getCleaner())) {
                            Instant instant2 = oldBooking.getDate().toInstant();
                            ZonedDateTime zdt2 = instant2.atZone(ZoneId.systemDefault());
                            LocalDate localDate2 = zdt2.toLocalDate();

                            List<Cleaner> cleaners = cleanerService.findAvailableCleanersForServiceAndTime(oldBooking.getStartTime(),oldBooking.getHours(),localDate2,oldBooking.getService().getId());
                            cleaners.add(booking.getCleaner());
                            redirectAttrs.addFlashAttribute("cleaners", cleaners);
                            List<Services> services = servicesService.getAllActiveServices();
                            redirectAttrs.addFlashAttribute("services", services);
                            redirectAttrs.addFlashAttribute("booking", oldBooking);
                            redirectAttrs.addFlashAttribute("message", "The previously assigned cleaner is not available at the new time with the old service.");
                            redirectAttrs.addFlashAttribute("alertType", "alert-danger");
                            return "redirect:/bookings/edit-booking/"+booking.getId();
                        }
                    }
                }

                // If the service has changed, check if the cleaner provides the new service
                if (!oldBooking.getService().equals(booking.getService())) {
                    List<Services> cleanerServices = booking.getCleaner().getServices().stream().toList();
                    if (!cleanerServices.contains(booking.getService())) {
                        Instant instant2 = oldBooking.getDate().toInstant();
                        ZonedDateTime zdt2 = instant2.atZone(ZoneId.systemDefault());
                        LocalDate localDate2 = zdt2.toLocalDate();

                        List<Cleaner> cleaners = cleanerService.findAvailableCleanersForServiceAndTime(oldBooking.getStartTime(),oldBooking.getHours(),localDate2,oldBooking.getService().getId());
                        cleaners.add(booking.getCleaner());
                        redirectAttrs.addFlashAttribute("cleaners", cleaners);
                        List<Services> services = servicesService.getAllActiveServices();
                        redirectAttrs.addFlashAttribute("services", services);
                        redirectAttrs.addFlashAttribute("booking", oldBooking);
                        redirectAttrs.addFlashAttribute("message", "The selected cleaner does not provide the requested service.");
                        redirectAttrs.addFlashAttribute("alertType", "alert-danger");
                        return "redirect:/bookings/edit-booking/"+booking.getId();
                    }
                }

                // If checks pass, proceed with updating the booking
                oldBooking.setCleaner(booking.getCleaner());
                oldBooking.setService(booking.getService());
                oldBooking.setStartTime(booking.getStartTime());
                oldBooking.setHours(booking.getHours());
                oldBooking.setPrice(booking.getService().getPrice());
                oldBooking.setAddress(booking.getAddress());
                oldBooking.setZipCode(booking.getZipCode());
                oldBooking.setPhone(booking.getPhone());
                oldBooking.setDate(booking.getDate());
                oldBooking.setFullName(booking.getFullName());

                bookingService.update(oldBooking);


                System.out.println(booking + "Edit");
                // bookingService.update(booking);
                redirectAttrs.addFlashAttribute("message", "The booking is updated successfully");
                redirectAttrs.addFlashAttribute("alertType", "alert-success");
            }else{
                redirectAttrs.addFlashAttribute("message", "The booking cannot be updated, it is not new");
                redirectAttrs.addFlashAttribute("alertType", "alert-danger");
            }
            }else{

            redirectAttrs.addFlashAttribute("message", "The booking cannot be found");
            redirectAttrs.addFlashAttribute("alertType", "alert-danger");
        }

        return "redirect:/bookings";
    }

}
