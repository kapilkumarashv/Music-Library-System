package songsmanagementsystem;
import java.sql.*;
import java.util.Scanner;
import java.util.Date;

public class MusicStreamingService {
    private static Scanner scanner = new Scanner(System.in);
    public static String currentUser = "";
    private static int currentPlaylistId = 1;
    public static void main(String[] args) {
        Connection conn = DBConnection.getConnection();
        UserFunctions userFunctions = new UserFunctions(conn, scanner);
        Show show = new Show(conn);
        System.out.print("Are you already logged in? (yes/no): ");
        String loggedIn = scanner.nextLine();
        if (loggedIn.equalsIgnoreCase("no")) {
            userFunctions.registerUser();
        } 
        else 
        {
            userFunctions.loginUser();
        }
        boolean a = true;
        while (a) {
            showMenu();
            int option = scanner.nextInt();
            scanner.nextLine();
            switch (option) {
                case 1:
                    addSong(conn);
                    break;
                case 2:
                    browseSongs(conn);
                    break;
                case 3:
                    createPlaylist(conn);
                    break;
                case 4:
                    show.showUserPlaylists(currentUser);
                    break;
                case 5:
                    accessPlaylist(conn);
                    break;
                case 6:
                    System.out.println("Exiting the application...");
                    a = false;
                    break;
                default:
                    System.out.println("Invalid option! Please try again.");
            }
        }

        DBConnection.closeConnection();
    }

    public static void showMenu() {
        System.out.println("\nMusic Streaming Service Menu:");
        System.out.println("1. Add a Song");
        System.out.println("2. Browse Songs");
        System.out.println("3. Create Playlist");
        System.out.println("4. Show My Playlists");
        System.out.println("5. Access Playlist");
        System.out.println("6. Exit");
        System.out.print("Choose an option: ");
    }

    public static void addSong(Connection conn) {
        System.out.print("Enter Song Title: ");
        String title = scanner.nextLine();

        System.out.print("Enter Artist Name: ");
        String artist = scanner.nextLine();

        System.out.print("Enter Album Name: ");
        String album = scanner.nextLine();

        System.out.print("Enter Genre: ");
        String genre = scanner.nextLine();

        System.out.print("Enter Duration (in format MM:SS): ");
        String duration = scanner.nextLine();

        System.out.print("Enter Release Year: ");
        int releaseYear = scanner.nextInt();
        scanner.nextLine();

        String sql = "INSERT INTO songs (title, artist, album, genre, duration, release_year) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, title);
            stmt.setString(2, artist);
            stmt.setString(3, album);
            stmt.setString(4, genre);
            stmt.setString(5, duration);
            stmt.setInt(6, releaseYear);

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Song added successfully!");
            } else {
                System.out.println("Failed to add the song. Please try again.");
            }
        } catch (SQLException e) {
            System.out.println("Error adding song: " + e.getMessage());
        }
    }

    public static void browseSongs(Connection conn) {
        String sql = "SELECT * FROM songs";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                System.out.println("Song ID: " + rs.getInt("song_id"));
                System.out.println("Title: " + rs.getString("title"));
                System.out.println("Artist: " + rs.getString("artist"));
                System.out.println("Album: " + rs.getString("album"));
                System.out.println("Genre: " + rs.getString("genre"));
                System.out.println("Duration: " + rs.getString("duration"));
                System.out.println("Release Year: " + rs.getInt("release_year"));
                System.out.println("--------------");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching songs: " + e.getMessage());
        }
    }

    public static void createPlaylist(Connection conn) {
        System.out.print("Enter Playlist Name: ");
        String name = scanner.nextLine();
        Date creationDate = new Date(System.currentTimeMillis());

        String userSql = "SELECT userid FROM users WHERE username = ?";
        try (PreparedStatement stmt = conn.prepareStatement(userSql)) {
            stmt.setString(1, currentUser);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int createdBy = rs.getInt("userid");
                String sql = "INSERT INTO playlists (name, created_by, is_premade, creation_date) VALUES (?, ?, true, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setString(1, name);
                    insertStmt.setInt(2, createdBy);
                    insertStmt.setDate(3, new java.sql.Date(creationDate.getTime()));

                    int rowsInserted = insertStmt.executeUpdate();
                    if (rowsInserted > 0) {
                        ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                        if (generatedKeys.next()) {
                            int playlistId = generatedKeys.getInt(1);
                            System.out.println("Playlist created successfully. Playlist ID: " + playlistId);

                            boolean addingSongs = true;
                            while (addingSongs) {
                                System.out.print("Enter Song ID to Add to Playlist (or type 'done' to stop): ");
                                String input = scanner.nextLine();

                                if (input.equalsIgnoreCase("done")) {
                                    addingSongs = false;
                                } else {
                                    try {
                                        int songId = Integer.parseInt(input);
                                        String insertSongSql = "INSERT INTO playlistsongs (playlist_id, song_id, added_date) VALUES (?, ?, ?)";
                                        try (PreparedStatement insertSongStmt = conn.prepareStatement(insertSongSql)) {
                                            insertSongStmt.setInt(1, playlistId);
                                            insertSongStmt.setInt(2, songId);
                                            insertSongStmt.setDate(3, new java.sql.Date(creationDate.getTime()));

                                            int songInserted = insertSongStmt.executeUpdate();
                                            if (songInserted > 0) {
                                                System.out.println("Song added to playlist successfully.");
                                            } else {
                                                System.out.println("Song ID " + songId + " not found.");
                                            }
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.println("Invalid Song ID. Please try again.");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error creating playlist: " + e.getMessage());
        }
    }
    public static void accessPlaylist(Connection conn) {
        System.out.print("Enter the Playlist Name you want to access: ");
        String playlistName = scanner.nextLine();
        String sql = "SELECT playlist_id FROM playlists WHERE name = ? AND created_by = (SELECT userid FROM users WHERE username = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playlistName);
            stmt.setString(2, currentUser);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int playlistId = rs.getInt("playlist_id");
                System.out.println("Displaying songs in the playlist: " + playlistName);
                Show show = new Show(conn);
                show.showSongsInPlaylist(playlistId);
            } else {
                System.out.println("No playlist found with the given name.");
            }
        } catch (SQLException e) {
            System.out.println("Error accessing playlist: " + e.getMessage());
        }
    }
}

class UserFunctions {
    private Connection conn;
    private Scanner scanner;

    public UserFunctions(Connection conn, Scanner scanner) {
        this.conn = conn;
        this.scanner = scanner;
    }

    public void registerUser() {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        System.out.print("Enter your email: ");
        String email = scanner.nextLine();
        System.out.print("Enter your password: ");
        String password = scanner.nextLine();
        Date joinDate = new Date(System.currentTimeMillis());

        String sql = "INSERT INTO users (username, email, password, join_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, password);
            stmt.setDate(4, new java.sql.Date(joinDate.getTime()));

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int userId = rs.getInt(1);
                    System.out.println("User registered successfully. User ID: " + userId);
                    MusicStreamingService.currentUser = username;
                }
            }
        } catch (SQLException e) {
            System.out.println("Error registering user: " + e.getMessage());
        }
    }

    public void loginUser() {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("Login successful. Welcome " + username + "!");
                MusicStreamingService.currentUser = username;
            } else {
                System.out.println("Invalid username or password.");
            }
        } catch (SQLException e) {
            System.out.println("Error logging in: " + e.getMessage());
        }
    }
}

class Show {
    private Connection conn;

    public Show(Connection conn) {
        this.conn = conn;
    }

    public void showUserPlaylists(String currentUser) {
        String sql = "SELECT * FROM playlists WHERE created_by = (SELECT userid FROM users WHERE username = ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentUser);
            ResultSet rs = stmt.executeQuery();

            boolean foundPlaylists = false;
            while (rs.next()) {
                foundPlaylists = true;
                int playlistId = rs.getInt("playlist_id");
                String playlistName = rs.getString("name");

                System.out.println("Playlist ID: " + playlistId);
                System.out.println("Playlist Name: " + playlistName);
                System.out.println("Songs in Playlist:");
                showSongsInPlaylist(playlistId);
                System.out.println("--------------");
            }

            if (!foundPlaylists) {
                System.out.println("You haven't created any playlists yet.");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching playlists: " + e.getMessage());
        }
    }

    public void showSongsInPlaylist(int playlistId) {
        String sql = "SELECT ps.song_id FROM playlistsongs ps WHERE ps.playlist_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, playlistId);
            ResultSet rs = stmt.executeQuery();

            boolean hasSongs = false;
            while (rs.next()) {
                hasSongs = true;
                int songId = rs.getInt("song_id");

                String songSql = "SELECT title, artist, album, genre, duration, release_year FROM songs WHERE song_id = ?";
                try (PreparedStatement songStmt = conn.prepareStatement(songSql)) {
                    songStmt.setInt(1, songId);
                    ResultSet songRs = songStmt.executeQuery();

                    if (songRs.next()) {
                        System.out.println("Song ID: " + songId);
                        System.out.println("Title: " + songRs.getString("title"));
                        System.out.println("Artist: " + songRs.getString("artist"));
                        System.out.println("Album: " + songRs.getString("album"));
                        System.out.println("Genre: " + songRs.getString("genre"));
                        System.out.println("Duration: " + songRs.getString("duration"));
                        System.out.println("Release Year: " + songRs.getInt("release_year"));
                        System.out.println("--------------");
                    }
                }
            }

            if (!hasSongs) {
                System.out.println("This playlist has no songs.");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching songs in playlist: " + e.getMessage());
        }
    }
}
