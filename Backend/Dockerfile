# Use official Node.js Alpine image for a lightweight container
FROM node:20-alpine

# Set the working directory
WORKDIR /usr/src/app

# Copy package dependencies first to leverage Docker layer caching
COPY package*.json ./

# Install dependencies exclusively for production
RUN npm ci --only=production

# Copy the actual application files
COPY server.js ./

# Expose the port Cloud Run expects
EXPOSE 8080

# Command to run the application
CMD [ "npm", "start" ]
