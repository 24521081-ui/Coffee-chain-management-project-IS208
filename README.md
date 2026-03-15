# Phung Loc Coffee App

Do an phat trien ung dung desktop quan ly chuoi cua hang ca phe.

JavaFX Desktop App using Maven and Oracle Database.

Run app:

```bat
.\run.bat
```

Test database:

```bat
.\test-db.bat
```

## Database configuration

After cloning the project, create a local database config file:

```bat
copy config.example.properties config.properties
```

Then edit only these values in `config.properties`:

```properties
db.username=PL_COFFEE
db.password=your_password_here
```

By default, the app connects to:

```text
localhost:1521/freepdb1
```

The JDBC URL is generated automatically as:

```text
jdbc:oracle:thin:@//localhost:1521/freepdb1
```

Normally you do not need to change host, port, or service. If your Oracle setup is different, set these optional values:

```properties
db.host=localhost
db.port=1521
db.service=freepdb1
```

Advanced users can still provide a full JDBC URL:

```properties
db.url=jdbc:oracle:thin:@//localhost:1521/freepdb1
```

When `db.url` is present, it overrides `db.host`, `db.port`, and `db.service`.

Do not commit `config.properties`; it contains local credentials and is ignored by Git.
