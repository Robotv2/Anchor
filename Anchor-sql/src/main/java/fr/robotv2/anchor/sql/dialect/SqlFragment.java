package fr.robotv2.anchor.sql.dialect;

import java.util.List;

public record SqlFragment(String sql, List<String> params) {
}
