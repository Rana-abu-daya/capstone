package org.ranaabudaya.capstone.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.ranaabudaya.capstone.dto.ServicesDTO;
import org.ranaabudaya.capstone.dto.UserDTO;
import org.ranaabudaya.capstone.entity.Admin;
import org.ranaabudaya.capstone.entity.Services;
import org.ranaabudaya.capstone.entity.User;
import org.ranaabudaya.capstone.repository.AdminRepository;
import org.ranaabudaya.capstone.repository.UserRepository;
import org.ranaabudaya.capstone.service.AdminService;
import org.ranaabudaya.capstone.service.RoleService;
import org.ranaabudaya.capstone.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Controller
@Slf4j
public class AdminController {

    AdminRepository adminRepository;
    UserService userService;
    AdminService adminService;
    private BCryptPasswordEncoder encoder;
    private RoleService roleService;

    @Autowired
    public AdminController(RoleService roleService,AdminRepository adminRepository, UserService userService, AdminService adminService, @Lazy BCryptPasswordEncoder encoder){
        this.adminRepository =adminRepository;
        this.userService=userService;
        this.adminService = adminService;
        this.encoder=encoder;
        this.roleService=roleService;
    }

    @GetMapping("/admins")
    private String AllAdmin(Model model, @RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 5); // get 5 items per page
        Page<Admin> adminList = adminService.getAllPagination(pageable);
        model.addAttribute("adminList", adminList);
        model.addAttribute("currentPage", page);
        return "admins";
    }

    @GetMapping("/admins/delete/{id}")
    @ResponseBody
    public String[] deleteAdminbyId(@PathVariable("id") int id, Model model, Principal principal) {

        String email = principal.getName();
        User user = userService.findUserByEmail(email);
        Admin admin= adminRepository.findByUserId(user.getId());
        if(admin.getId() == id){
            String arr[] = new String[2];
            arr[0] = "You can't delete yourself. the logged admin";
            arr[1]= "danger";
            return  arr;
        }
        int result =  adminService.deleteById(id);
        String arr[] = new String[2];
        if(result>0){
            arr[0] = "The admin is deleted successfully";
            arr[1]= "success";
            return  arr;

        }else {
            arr[0] = "The admin's deletion failed, there is one admin";
            arr[1]= "danger";
            return  arr;

        }

    }
    @GetMapping("/admins/edit-admin/{id}")
    public String editAdminbyId(@PathVariable("id") int id, Model model) {
        Optional<Admin> admin = adminService.findAdminById(id);
        model.addAttribute("admin",admin);
        model.addAttribute("id", id);
        return "edit-admin";
    }
    @PostMapping("/admins/updateAdmin/{id}")
    public String updateServices(@PathVariable("id") int id, @Valid @ModelAttribute ("admin") Admin admin, BindingResult bindingResult, Model model, RedirectAttributes redirectAttrs) {
        System.out.println(admin.toString());
        System.out.println(admin.getUser().toString());
        if(bindingResult.hasErrors())

        {

            model.addAttribute("id", id);
            // log.warn("Wrong attempt");
            return "edit-admin";
        }

        Optional<Admin> admin1 = adminService.findAdminById(id);
        if (admin1.isPresent()) {

            if(userService.findUserByEmail(admin.getUser().getEmail()) != null && userService.findUserByEmail(admin.getUser().getEmail()).getId() != admin1.get().getUser().getId())
            {
                model.addAttribute("duplicateEmail","Email is used in Homey" );
                return "edit-admin";
            }
         ModelMapper modelMapper = new ModelMapper();

            UserDTO userDTO = modelMapper.map(admin.getUser(), UserDTO.class);
            userDTO.setRoleName("ROLE_ADMIN");
            userDTO.setId(admin1.get().getUser().getId());
            userDTO.setPassword(admin1.get().getUser().getPassword());
           int idUser =  userService.update(userDTO);
            admin.setUser(userService.findById(idUser).get());

            adminRepository.save(admin);
            redirectAttrs.addFlashAttribute("message", "Admin is updated successfully");
            redirectAttrs.addFlashAttribute("alertType", "alert-success");
        }else{

            redirectAttrs.addFlashAttribute("message", " Admin cannot be found");
            redirectAttrs.addFlashAttribute("alertType", "alert-danger");
        }

        return "redirect:/admins";

    }



}
