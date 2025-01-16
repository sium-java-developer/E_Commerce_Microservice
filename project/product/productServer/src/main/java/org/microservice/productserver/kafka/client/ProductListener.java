package org.microservice.productserver.kafka.client;


import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.microservice.productserver.model.Product;
import org.microservice.productserver.model.ProductOR;
import org.microservice.productserver.model.Products;
import org.microservice.productserver.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ProductListener {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductMapper productMapper;

    private static final Logger logger = LoggerFactory.getLogger(ProductListener.class);

    @KafkaListener(topics = "${kafka.topic.product.request}",
            containerFactory = "requestReplyListenerContainerFactory")
    @SendTo
    public Products listenConsumerRecord(ConsumerRecord<String, Products> record){
        long secondsToSleep = 3;
        logger.info("start");

        // print all headers
        record.headers().forEach(header -> logger.debug(header.key() + " : " + new String(header.value())));
        String key = record.key();
        Products products = record.value();
        logger.debug("listen key: " + key);
        logger.debug("listen value: " + products);

        Products productsToReturn = resolveAndExecute(products);
        logger.info("ending");
        return productsToReturn;
    }

    private Products resolveAndExecute(Products products){
        logger.info("start");
        Products productsToReturn = null;

        if (products.getOperation().equals(Products.RETRIEVE_DETAILS)){
            productsToReturn = getProduct(products);
        }
        else if (products.getOperation().equals(Products.RETRIEVE_ALL)){
            productsToReturn = getAllProducts(products);
        }
        else if (products.getOperation().equals(Products.CREATE)){
            productsToReturn = createProduct(products);
        }
        else if (products.getOperation().equals(Products.UPDATE)){
            productsToReturn = updateProduct(products);
        }
        else if (products.getOperation().equals(Products.DELETE)){
            productsToReturn = deleteProduct(products);
        }
        else {
            logger.info("undefined operation.");
        }
        logger.debug("end");
        return productsToReturn;
    }

    // retrieve a product
    private Products getProduct(Products products){
        logger.info("start");
        Products productsToReturn = new Products();
        if ((products != null) && (products.getProducts().iterator().hasNext())){
            String productId = ((Product) products.getProducts().iterator().next()).getProductId();
            logger.debug("fetching product with product id {}", productId);
            ProductOR productOR = productRepository.findById(Long.parseLong(productId)).get();
            productsToReturn.setOperation(Products.SUCCESS);
            if (productOR == null){
                logger.debug("product with productId {} not found in repository", productId);
            }
            else {
                logger.debug("product with productId {} found in repository", productId);
                List<Product> productListToReturn = new ArrayList<>();
                productListToReturn.add(productMapper.entityToApi(productOR));
                productsToReturn.setProducts(productListToReturn);
            }
        }
        else {
            logger.debug("product can not be fetched, since param is null or empty");
            productsToReturn.setOperation(Products.FAILURE);
        }
        logger.debug("ending");
        return productsToReturn;
    }

    // retrieve all products
    private Products getAllProducts(Products products){
        logger.info("start");
        Products productsToReturn = new Products();
        Iterable<ProductOR> iterable = productRepository.findAll();
        List<Product> productListToReturn = new ArrayList<Product>();
        for (ProductOR productOR : iterable){
            productListToReturn.add(productMapper.entityToApi(productOR));
        }
        if (productListToReturn.size() == 0){
            logger.debug("no products retrieved from repo.");
        }
        productListToReturn.forEach(item -> logger.debug(item.toString()));

        productsToReturn.setOperation(Products.SUCCESS);
        productsToReturn.setProducts(productListToReturn);
        logger.debug("ending");
        return productsToReturn;
    }

    // create a product
    private Products createProduct(Products products){
        logger.info("start");
        Products productsToReturn = new Products();
        List<Product> productsListToReturn = null;
        Product productToCreate = null;
        List<ProductOR> productsFound = null;
        ProductOR productCreatedOr = null;

        if ((products != null) && (products.getProducts().iterator().hasNext())){
            productToCreate = products.getProducts().iterator().next();
            logger.debug("Attempting to create a product with code {}", productToCreate.getCode());
            productsFound = productRepository.findByCode(productToCreate.getCode());
            if (productsFound.size() > 0){
                logger.debug("A product with code {} already exists", productsFound.iterator().next().getCode());
                productsToReturn.setOperation(Products.FAILURE);
            }
            else {
                productCreatedOr = productRepository.save(productMapper.apiToEntity(productToCreate));
                logger.debug("a product with id {} created ", productCreatedOr.getProductId());
                productsToReturn.setOperation(Products.SUCCESS);
                productsListToReturn = new ArrayList<>();
                productsListToReturn.add(productMapper.entityToApi(productCreatedOr));
                productsToReturn.setProducts(productsListToReturn);
            }
        }
        else {
            logger.debug("products can not be created since param is null or empty.");
            productsToReturn.setOperation(Products.FAILURE);
        }
        logger.info("ending");
        return productsToReturn;
    }

    // update a product
    private Products updateProduct(Products products) {
        logger.info("Start");
        Products productsToReturn = new Products();
        List<Product> productListToReturn = null;
        Product productToUpdate = null;
        ProductOR productFoundOR = null;
        ProductOR productUpdatedOR = null;

        if((products != null) && (products.getProducts().iterator().hasNext())) {
            productToUpdate = products.getProducts().iterator().next();
            logger.debug("Attempting to find a Product with id: {} to update", productToUpdate.getProductId());

            productFoundOR = productRepository.findById(Long.parseLong(productToUpdate.getProductId())).get();
            if (null != productFoundOR) {
                logger.debug("A Product with id {} exist, attempting to update", productFoundOR.getProductId());
                productsToReturn.setOperation(Products.SUCCESS);
                productUpdatedOR = productRepository.save(productMapper.apiToEntity(productToUpdate));
                productListToReturn = new ArrayList<Product>();
                productListToReturn.add(productMapper.entityToApi(productUpdatedOR));
                productsToReturn.setProducts(productListToReturn);
            }
            else {
                logger.debug("A Product with id {} doesn't exist", productToUpdate.getProductId());
                productsToReturn.setOperation(Products.FAILURE);
            }
        }
        else {
            logger.debug("Product cannot be updated, since param is null or empty");
            productsToReturn.setOperation(Products.FAILURE);
        }
        logger.info("Ending");
        return productsToReturn;
    }

    // delete product
    private Products deleteProduct(Products products) {

        logger.info("Start");
        Products productsToReturn = new Products();
        List<Product> productListToReturn = null;
        Product productToDelete = null;
        ProductOR productFoundOR = null;

        if((products != null) && (products.getProducts().iterator().hasNext())) {

            productToDelete = products.getProducts().iterator().next();
            logger.debug("Attempting to find a Product with id: {} to delete", productToDelete.getProductId());

            productFoundOR = productRepository.findById(Long.parseLong(productToDelete.getProductId())).get();
            if (productFoundOR != null) {
                logger.debug("A Product with id {} exist, attempting to delete", productFoundOR.getProductId());
                productRepository.delete(productMapper.apiToEntity(productToDelete));
                productsToReturn.setOperation(Products.SUCCESS);
                productListToReturn = new ArrayList<Product>();
                productsToReturn.setProducts(productListToReturn);
            }
            else {
                logger.debug("A Product with id {} doesn't exist", productToDelete.getProductId());
                productsToReturn.setOperation(Products.FAILURE);
            }
        }
        else {
            logger.debug("Product cannot be deleted, since param is null or empty");
            productsToReturn.setOperation(Products.FAILURE);
        }
        logger.info("Ending");
        return productsToReturn;
    }

    private void delay() {
        long secondsToSleep = 1;
        logger.info("Start");
        logger.debug(Thread.currentThread().toString());
        logger.debug("Starting to Sleep Seconds : " + secondsToSleep);

        try{
            Thread.sleep(1000 * secondsToSleep);
        }
        catch(Exception e) {
            logger.error("Error : " + e);
        }
        logger.debug("Awakening from Sleep...");
    }
}
