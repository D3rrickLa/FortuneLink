package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import java.util.List;

public interface Projector<P, T> {
  P project(List<T> transactions);
}
