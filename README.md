# Doc Transformer

Document transformation and manipulation service supporting PDF, Office, and CAD file conversions.

## Features

- **Image to PDF**: Convert images (PNG, JPG, etc.) to PDF format
- **Office to PDF**: Convert Office documents (Word, Excel, PowerPoint) to PDF using LibreOffice
- **CAD to PDF**: Convert CAD files to PDF using Apryse SDK
- **PDF Operations**: Various PDF manipulation capabilities

## Prerequisites

- Docker and Docker Compose
- (Optional) Java 17+ and Maven for local development

## Quick Start

### Using Docker Compose (Recommended)

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd doc-transformer
   ```

2. **Start the application**
   ```bash
   docker-compose up --build
   ```

3. **Access the application**
   - Application: http://localhost:8080
   - Debug port: localhost:5005 (for IDE debugging)
   - Health check: http://localhost:8080/actuator/health

### Configuration

#### License Key Configuration

The application uses the Apryse (PDFTron) SDK which requires a license key. The license key can be configured in several ways:

1. **Default**: Uses the key configured in `src/main/resources/application.yml`
2. **Environment Variable**: Set `PDFNET_LICENSE_KEY` environment variable
3. **Docker Compose**: Create a `.env` file (see `.env.example`)

**Example `.env` file:**
```bash
PDFNET_LICENSE_KEY=your-license-key-here
```

### API Endpoints

#### Document Conversion

- `POST /convert/image-to-pdf` - Convert image files to PDF
- `POST /convert/office-to-pdf` - Convert Office documents (Word, Excel, PowerPoint) to PDF  
- `POST /convert/cad-to-pdf` - Convert CAD files to PDF

#### PDF Manipulation

- `POST /split` - Split PDF pages (parameters: `file`, `from`, `to`)
- `POST /merge` - Merge two PDF files (parameters: `file1`, `file2`)
- `POST /reorder` - Reorder PDF pages (parameters: `file`, `order` e.g., "3,1,2")
- `POST /rotate` - Rotate PDF pages (parameters: `file`, `pages`, `degrees`)
- `POST /remove` - Remove PDF pages (parameters: `file`, `pages` e.g., "2,4,7")
- `POST /add` - Add blank pages or append PDFs (parameters: `file`, `blanks`, `append[]`)

#### System

- `GET /` - Redirects to index.html
- `GET /actuator/health` - Health check endpoint

### Development Setup

#### Local Development

1. **Prerequisites**
   ```bash
   # Install Java 17+
   # Install Maven 3.6+
   # Install LibreOffice (for office conversions)
   ```

2. **Run locally**
   ```bash
   mvn spring-boot:run
   ```

3. **Build JAR**
   ```bash
   mvn clean package
   java -jar target/doc-transformer-0.0.1-SNAPSHOT.jar
   ```

#### Debug Mode

When running with Docker Compose, the application automatically starts with debug mode enabled on port 5005.

**Connect your IDE:**
- Host: `localhost`
- Port: `5005`
- Transport: `Socket`
- Debugger mode: `Attach`

### Docker Commands

```bash
# Build and start services
docker-compose up --build

# Start in background
docker-compose up -d --build

# View logs
docker-compose logs -f

# Stop services
docker-compose down

# Rebuild and restart
docker-compose down && docker-compose up --build
```

### API Usage Examples

#### Document Conversion

```bash
# Convert image to PDF
curl -X POST -F "file=@image.png" http://localhost:8080/convert/image-to-pdf --output result.pdf

# Convert Office document to PDF
curl -X POST -F "file=@document.docx" http://localhost:8080/convert/office-to-pdf --output result.pdf

# Convert CAD file to PDF
curl -X POST -F "file=@drawing.dwg" http://localhost:8080/convert/cad-to-pdf --output result.pdf
```

#### PDF Manipulation

```bash
# Split PDF pages (extract pages 2-5)
curl -X POST -F "file=@document.pdf" -F "from=2" -F "to=5" http://localhost:8080/split --output pages_2_5.pdf

# Merge two PDFs
curl -X POST -F "file1=@doc1.pdf" -F "file2=@doc2.pdf" http://localhost:8080/merge --output merged.pdf

# Reorder PDF pages (move page 3 to first, then page 1, then page 2)
curl -X POST -F "file=@document.pdf" -F "order=3,1,2" http://localhost:8080/reorder --output reordered.pdf

# Rotate pages 1 and 3 by 90 degrees
curl -X POST -F "file=@document.pdf" -F "pages=1,3" -F "degrees=90" http://localhost:8080/rotate --output rotated.pdf

# Remove pages 2 and 4
curl -X POST -F "file=@document.pdf" -F "pages=2,4" http://localhost:8080/remove --output removed.pdf

# Add 2 blank pages to a PDF
curl -X POST -F "file=@document.pdf" -F "blanks=2" http://localhost:8080/add --output with_blanks.pdf
```

### Troubleshooting

#### Common Issues

1. **License Key Issues**
   - Ensure your Apryse license key is valid and properly configured
   - Check the application logs for license-related errors

2. **LibreOffice Conversion Failures**
   - Verify LibreOffice is installed in the container
   - Check file format is supported by LibreOffice

3. **Memory Issues**
   - Increase Docker memory limits for large file processing
   - Monitor container resource usage

#### Health Check

```bash
# Check application health
curl http://localhost:8080/actuator/health

# Expected response:
# {"status":"UP"}
```

#### Logs

```bash
# View application logs
docker-compose logs doc-transformer

# Follow logs in real-time
docker-compose logs -f doc-transformer
```

### Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `mvn test`
5. Format code: `mvn spotless:apply`
6. Submit a pull request

### License

[Add your license information here]
