package com.rachnicrice.spordering.controllers;


import com.rachnicrice.spordering.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@Controller
public class ProductController {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    ApplicationUserRepository applicationUserRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    LineItemRepository lineItemRepository;


    @GetMapping("/products")
    public String showPage(Principal p, Model model, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "item_id") String sortBy) {
//        PageRequest pagereq = PageRequest.of(page,4, Sort.by(sortBy).ascending());

        if(p != null) {
            System.out.println(p.getName()+" is logged in!");
            model.addAttribute("username", p.getName());
        } else {
            System.out.println("nobody is logged in");
        }


        model.addAttribute("data", productRepository.findAll());
//        System.out.println("FIND PRODUCT ID"+productRepository.);
        model.addAttribute("currentPage",page);
        return "products";
    }

    @PostMapping("/mycart")
    public RedirectView addCart(Model model, Principal p, Long item_id, int quantity) {
        System.out.println(item_id);
        System.out.println(quantity);

        if (p != null) {
            System.out.println(p.getName()+" is logged in!");
            model.addAttribute("username", p.getName());
        } else {
            System.out.println("nobody is logged in");
        }

        Timestamp createdAt = new Timestamp(System.currentTimeMillis());

        ApplicationUser loggedInUser = applicationUserRepository.findByUsername(p.getName());

        List<Order> userOrders = loggedInUser.getOrders();

        boolean startedAtLeastOneOrder = userOrders!=null;


        // Initialize as as true (case that does not have any unsubmitted orders), set to false if one is found in the loop
        boolean onlySubmittedOrders = true;
        if (userOrders!=null) {
            for (Order order : userOrders) {
                if (order.getSubmitted()==false) {
                    onlySubmittedOrders = false;
                }
            }
        }

        if (!startedAtLeastOneOrder || onlySubmittedOrders) {
            Order order = new Order(loggedInUser, createdAt,false);
            orderRepository.save(order);
            //change hard coded 1 and 10 to path variables
            LineItem cartItem = new LineItem(order, productRepository.getOne(item_id), quantity);// create new cart item with order, product, and quantity
            lineItemRepository.save(cartItem);
        } else {
            for (Order order : userOrders) {
                if (order.getSubmitted()==false) {
                    Order unsubmittedOrder = order;
                    //change hard coded 1 and 10 to path variables
                    LineItem cartItem = new LineItem(unsubmittedOrder, productRepository.getOne(item_id), quantity);// create new cart item with order, product, and quantity
                    lineItemRepository.save(cartItem);
                }
            }

        }

        return new RedirectView("/mycart");
    }


    @PostMapping("/save")
    public RedirectView save(Product product) {
        productRepository.save(product);

        return new RedirectView("/product");
    }

    @GetMapping("/delete")
    public RedirectView delete(Long id) {
        productRepository.deleteById(id);

        return new RedirectView("/product");
    }

    @GetMapping("/findOne")
    @ResponseBody
    public Product findOne(Long id) {
        return productRepository.getOne(id);

    }

    @GetMapping("/populateDatabase")
    public RedirectView populateDatabase(Principal p) {
        if (p != null) {
            System.out.println(p.getName() + " is logged in!");
        } else {
            System.out.println("nobody is logged in");
        }

        // Make a list of products already in the database
        List<Product> dbProductList = productRepository.findAll();

        HashSet<String> dbItemCodeSet = new HashSet<>();
        for (Product product : dbProductList) {
            dbItemCodeSet.add(product.getItemCode());
        }

        // make a set of products to add
        HashSet<Product> productsToAdd = new HashSet<>();
        productsToAdd.add(new Product("DS2310BLK-LF", "Downspout", "2x3", "10'", "Black", "style", "downspout", 10));
        productsToAdd.add(new Product("DS2310WMUS-LF", "Downspout", "2x3", "10'", "Musket Brown", "style", "downspout", 10));
        productsToAdd.add(new Product("DS2310LGWHT-LF", "Downspout", "2x3", "10'", "Low-Gloss White", "style", "downspout", 10));
        productsToAdd.add(new Product("DS2310WHT-LF", "Downspout", "2x3", "10'", "High Gloss White", "style", "downspout", 10));
        productsToAdd.add(new Product("DS3410LGWHT-LF", "Downspout", "3x4", "10'", "Low-Gloss White", "style", "downspout", 10));

        // for each product in the set, add it to the database only if there isn't already a product with that item code in the database
        for (Product product : productsToAdd) {
            if (!dbItemCodeSet.contains(product.getItemCode())) {
                productRepository.save(product);
            }
        }

        return new RedirectView("/");
    }
}
