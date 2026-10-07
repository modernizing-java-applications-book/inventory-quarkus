package com.redhat.cloudnative;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import jakarta.persistence.Column;

@Entity 
@Table(name = "INVENTORY") 
public class Inventory  extends PanacheEntity{

    @Column
    public int quantity;


    @Override
    public String toString() {
        return "Inventory [Id='" + id + '\'' + ", quantity=" + quantity + ']';
    }
}
