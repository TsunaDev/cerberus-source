/*
 * Cerberus  Copyright (C) 2013  vertigo17
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This file is part of Cerberus.
 *
 * Cerberus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Cerberus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Cerberus.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cerberus.crud.entity;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author bcivel
 */
public class SessionCounterListener implements HttpSessionListener {

    @Autowired
    SessionCounter cs;
    
@Override
    public void sessionCreated(HttpSessionEvent arg0) {
        WebApplicationContextUtils
        .getRequiredWebApplicationContext(arg0.getSession().getServletContext())
        .getAutowireCapableBeanFactory()
        .autowireBean(this);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent arg0) {
        //TODO:FN remove log messaegs
        org.apache.log4j.Logger.getLogger(SessionCounterListener.class.getName()).log(org.apache.log4j.Level.WARN, "DESTROY SESSION");
        if(arg0.getSession() == null){
            org.apache.log4j.Logger.getLogger(SessionCounterListener.class.getName()).log(org.apache.log4j.Level.FATAL, "SESSION IS NULL");
        }else{
            org.apache.log4j.Logger.getLogger(SessionCounterListener.class.getName()).log(org.apache.log4j.Level.FATAL, "SESSIONID " + (String) arg0.getSession().getId());
        }
        String key = (String) arg0.getSession().getId();
        cs.destroyUser(key);
    }
}
