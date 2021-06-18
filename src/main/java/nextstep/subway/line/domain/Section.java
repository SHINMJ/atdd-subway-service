package nextstep.subway.line.domain;

import nextstep.subway.line.ui.InvalidDistanceException;
import nextstep.subway.station.domain.Station;

import javax.persistence.*;

@Entity
public class Section {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "line_id")
    private Line line;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "up_station_id")
    private Station upStation;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "down_station_id")
    private Station downStation;

    private int distance;

    public Section() {
    }

    public Section(Station upStation, Station downStation, int distance) {
        this.upStation = upStation;
        this.downStation = downStation;
        this.distance = distance;
    }

    public Section(Line line, Station upStation, Station downStation, int distance) {
        this.line = line;
        this.upStation = upStation;
        this.downStation = downStation;
        this.distance = distance;
    }

    public Long getId() {
        return id;
    }

    public Line getLine() {
        return line;
    }

    public Station upStation() {
        return upStation;
    }

    public Station downStation() {
        return downStation;
    }

    public int getDistance() {
        return distance;
    }

    public void updateUpStation(Station station, int newDistance) {
        if (this.distance <= newDistance) {
            throw new InvalidDistanceException();
        }
        this.upStation = station;
        this.distance -= newDistance;
    }

    public void updateDownStation(Station station, int newDistance) {
        if (this.distance <= newDistance) {
            throw new InvalidDistanceException();
        }
        this.downStation = station;
        this.distance -= newDistance;
    }

    public void changeDownStationWhenRemove(Station station, int newDistance) {
        this.downStation = station;
        this.distance = newDistance;
    }

    public boolean hasSameUpStation(Station station) {
        return this.upStation.equals(station);
    }

    public boolean hasSameDownStation(Station station) {
        return this.downStation.equals(station);
    }

    public void applyLine(Line line) {
        this.line = line;
    }
}
