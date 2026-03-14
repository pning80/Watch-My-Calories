# Use official Node.js Alpine image for a lightweight container
FROM node:20-alpine

# Set the working directory
WORKDIR /usr/src/app

# Copy package dependencies first to leverage Docker layer caching
COPY package*.json ./

# Install all dependencies (includes TypeScript for build step)
RUN npm ci

# Copy source files and config
COPY tsconfig.json server.ts ./
COPY src/ ./src/
COPY apple_attest_root_ca.pem ./

# Compile TypeScript
RUN npx tsc

# Remove dev dependencies to shrink image
RUN npm prune --production

# Expose the port Cloud Run expects
EXPOSE 8080

# Command to run the application
CMD [ "npm", "start" ]
