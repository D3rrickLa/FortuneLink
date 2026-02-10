package com.laderrco.fortunelink.portfolio_management.domain.services.projectors;

import java.util.List;

public interface Projector<P, T> {
    P project(List<T> transactions);

}
