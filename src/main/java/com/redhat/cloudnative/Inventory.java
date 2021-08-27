package com.redhat.cloudnative;

import javax.persistence.Entity;
import javax.persistence.Table;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

import javax.persistence.Column;

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
